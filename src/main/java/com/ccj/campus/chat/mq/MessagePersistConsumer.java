package com.ccj.campus.chat.mq;

import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消息持久化消费者。对齐论文 5.2：
 * "无论接收方是否在线，消息均会被异步提交至 RocketMQ 的持久化主题，
 *  消费者负责将其写入 PostgreSQL 消息表，这一写入操作在推送路径之外进行，
 *  确保不增加推送延迟。"
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQTopic.MSG_PERSIST_TOPIC,
        consumerGroup = "campus_chat_msg_persist_group"
)
public class MessagePersistConsumer implements RocketMQListener<String> {

    private final ObjectMapper mapper;
    private final MessageService messageService;

    @Override
    public void onMessage(String json) {
        try {
            ChatMessageDTO msg = mapper.readValue(json, ChatMessageDTO.class);
            // 幂等：在 service 内基于 (from_uid, room_id, client_seq) 唯一约束去重
            messageService.persist(msg);
        } catch (Exception e) {
            // 消费失败 throw 让 MQ 自动重试（RocketMQ 默认 16 次指数退避）
            log.error("persist consumer error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}