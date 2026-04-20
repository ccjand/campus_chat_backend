package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.Contact;
import com.ccj.campus.chat.entity.Message;
import com.ccj.campus.chat.entity.MessageRead;
import com.ccj.campus.chat.mapper.ContactMapper;
import com.ccj.campus.chat.mapper.MessageMapper;
import com.ccj.campus.chat.mapper.MessageReadMapper;
import com.ccj.campus.chat.mq.OfflineMessageConsumer;
import com.ccj.campus.chat.service.BadgeService;
import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息业务实现。对齐论文 5.2 "异步持久化、不阻塞推送链路"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final MessageReadMapper readMapper;
    private final ContactMapper contactMapper;
    private final OnlineUserService onlineUserService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final BadgeService badgeService;

    /**
     * 对齐论文 3.2: 2 分钟内可撤回
     */
    @Value("${campus.message.recall-window-seconds:120}")
    private long recallWindowSeconds;

    @Override
    public Long prepareMessageId() {
        Long id = redisTemplate.opsForValue().increment("msg:id:gen");
        if (id == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
        return id;
    }

    @Override
    public void markRoomRead(Long roomId, Long readerId) {
        // 直接把 last_read_id 更新为当前 last_msg_id
        contactMapper.markRoomRead(roomId, readerId);
    }

    @Override
    @Transactional
    public void persist(ChatMessageDTO dto) {
        Message m = new Message();
        m.setRoomId(dto.getRoomId());
        m.setFromUid(dto.getFromUid());
        m.setType(dto.getType() == null ? Message.TYPE_TEXT : dto.getType());
        m.setContent(dto.getContent());
        m.setExtInfo(dto.getExtInfo() == null ? new HashMap<>() : dto.getExtInfo());
        m.setClientSeq(dto.getClientSeq());
        m.setStatus(Message.STATUS_NORMAL);
        m.setCreateTime(dto.getCreateTime() == null ? LocalDateTime.now() : dto.getCreateTime());
        try {
            messageMapper.insert(m);
        } catch (DuplicateKeyException dup) {
            // 对齐论文 4.2: 基于客户端序列号幂等，重复投递直接忽略
            log.debug("duplicate message ignored: uid={}, seq={}", dto.getFromUid(), dto.getClientSeq());
        }
    }

    @Override
    public void updateLastMsg(Long roomId, Long msgId) {
        UpdateWrapper<Contact> w = new UpdateWrapper<>();
        w.eq("room_id", roomId)
                .set("last_msg_id", msgId)
                .set("active_time", LocalDateTime.now());
        contactMapper.update(null, w);
    }

    @Override
    @Transactional
    public void recall(Long msgId, Long uid) {
        Message m = messageMapper.selectById(msgId);
        if (m == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!m.getFromUid().equals(uid)) throw new BusinessException(ResultCode.MSG_NOT_OWNER);

        Duration elapsed = Duration.between(m.getCreateTime(), LocalDateTime.now());
        if (elapsed.getSeconds() > recallWindowSeconds) {
            throw new BusinessException(ResultCode.MSG_RECALL_TIMEOUT);
        }
        int n = messageMapper.recall(msgId, uid);
        if (n == 0) throw new BusinessException(ResultCode.MSG_RECALL_TIMEOUT);

        // 广播房间，让双方 UI 同步
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "recall");
        evt.put("msgId", msgId);
        evt.put("roomId", m.getRoomId());
        onlineUserService.broadcast("/topic/room/" + m.getRoomId(), evt);
    }

    @Override
    @Transactional
    public void markRead(Long roomId, Long msgId, Long readerId) {
        if (msgId == null) return;
        MessageRead r = new MessageRead();
        r.setMsgId(msgId);
        r.setRoomId(roomId);
        r.setReaderId(readerId);
        r.setReadTime(LocalDateTime.now());
        try {
            readMapper.insert(r);
        } catch (DuplicateKeyException ignore) {
        }

        UpdateWrapper<Contact> w = new UpdateWrapper<>();
        w.eq("user_id", readerId).eq("room_id", roomId).set("last_read_id", msgId);
        contactMapper.update(null, w);

        // 通知发送方：接收方已读
        Message m = messageMapper.selectById(msgId);
        if (m != null && onlineUserService.isOnline(m.getFromUid())) {
            Map<String, Object> evt = new HashMap<>();
            evt.put("event", "read");
            evt.put("msgId", msgId);
            evt.put("roomId", roomId);
            evt.put("readerId", readerId);
            onlineUserService.push(m.getFromUid(), "/queue/receipts", evt);
        }

        // 已读后刷新自己的消息 tab 未读数
        badgeService.pushBadgeIfOnline(readerId, null);
    }

    @Override
    public List<Message> pullHistory(Long roomId, LocalDateTime cursor, int size) {
        return messageMapper.pullHistory(roomId, cursor, Math.min(size, 100));
    }

    @Override
    public List<ChatMessageDTO> pullOffline(Long uid, int max) {
        String key = OfflineMessageConsumer.OFFLINE_QUEUE_PREFIX + uid;
        Set<String> payloads = redisTemplate.opsForZSet().range(key, 0, max);
        if (payloads == null || payloads.isEmpty()) return List.of();

        List<ChatMessageDTO> list = payloads.stream().map(json -> {
            try {
                return objectMapper.readValue(json, ChatMessageDTO.class);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        redisTemplate.delete(key);
        return list;
    }
}