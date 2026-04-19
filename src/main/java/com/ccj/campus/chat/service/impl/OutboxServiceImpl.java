package com.ccj.campus.chat.service.impl;

import com.ccj.campus.chat.entity.MessageOutbox;
import com.ccj.campus.chat.mapper.MessageOutboxMapper;
import com.ccj.campus.chat.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 服务实现。
 *
 * 入队：业务事务内同库写入（Propagation.MANDATORY 强制共用事务，保证原子性）
 * 发送：独立 @Scheduled 定时任务扫描并发送，失败指数退避重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private static final int BATCH = 200;
    private static final int MAX_RETRY = 8;

    private final MessageOutboxMapper mapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, String tag, Object payload) {
        try {
            MessageOutbox e = new MessageOutbox();
            e.setTopic(topic);
            e.setTag(tag);
            e.setPayload(objectMapper.writeValueAsString(payload));
            e.setStatus(MessageOutbox.STATUS_PENDING);
            e.setRetryCount(0);
            e.setCreateTime(LocalDateTime.now());
            mapper.insert(e);
        } catch (JsonProcessingException ex) {
            log.error("outbox enqueue failed: topic={}, err={}", topic, ex.getMessage());
            throw new RuntimeException(ex);
        }
    }

    /**
     * 定时扫描 + 发送。每 1 秒跑一次。
     * 多节点部署时应配合分布式锁避免重复消费。
     */
    @Scheduled(fixedDelay = 1000)
    public void dispatch() {
        List<MessageOutbox> pending = mapper.scanPending(LocalDateTime.now(), BATCH);
        if (pending.isEmpty()) return;

        for (MessageOutbox e : pending) {
            try {
                String dest = e.getTopic() + (e.getTag() == null ? "" : ":" + e.getTag());
                SendResult sr = rocketMQTemplate.syncSend(
                        dest, MessageBuilder.withPayload(e.getPayload()).build());
                if (sr != null && sr.getSendStatus() == SendStatus.SEND_OK) {
                    markSent(e.getId());
                } else {
                    markFailed(e, "sendStatus=" + (sr == null ? "null" : sr.getSendStatus()));
                }
            } catch (Exception ex) {
                markFailed(e, ex.getMessage());
            }
        }
    }

    @Transactional
    public void markSent(Long id) {
        MessageOutbox u = new MessageOutbox();
        u.setId(id);
        u.setStatus(MessageOutbox.STATUS_SENT);
        u.setSendTime(LocalDateTime.now());
        mapper.updateById(u);
    }

    @Transactional
    public void markFailed(MessageOutbox e, String err) {
        int retry = e.getRetryCount() + 1;
        MessageOutbox u = new MessageOutbox();
        u.setId(e.getId());
        u.setRetryCount(retry);
        u.setStatus(MessageOutbox.STATUS_FAILED);
        if (retry >= MAX_RETRY) {
            log.error("outbox giveup: id={}, err={}", e.getId(), err);
        } else {
            // 指数退避：2, 4, 8, 16, 32, 64, 128, 256 秒
            long backoff = (long) Math.pow(2, retry);
            u.setNextRetryTime(LocalDateTime.now().plusSeconds(backoff));
            log.warn("outbox retry: id={}, retry={}, backoff={}s, err={}",
                    e.getId(), retry, backoff, err);
        }
        mapper.updateById(u);
    }
}