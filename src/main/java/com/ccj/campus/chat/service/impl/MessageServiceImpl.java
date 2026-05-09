package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.*;
import com.ccj.campus.chat.mapper.*;
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

    private final ChatRoomMapper chatRoomMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;

    /**
     * 对齐论文 3.2: 2 分钟内可撤回
     */
    @Value("${campus.message.recall-window-seconds:120}")
    private long recallWindowSeconds;

    @Override
    public Long prepareMessageId() {
        Long id = messageMapper.nextMessageId();
        if (id == null) throw new BusinessException(ResultCode.INTERNAL_ERROR);
        return id;
    }

    @Override
    public void markRoomRead(Long roomId, Long readerId) {
        // 直接把 last_read_id 更新为当前 last_msg_id
        contactMapper.markRoomRead(roomId, readerId);
    }


    @Override
    @Transactional
    public boolean persist(ChatMessageDTO dto) {
        Message m = new Message();
        m.setId(dto.getId());
        m.setRoomId(dto.getRoomId());
        m.setFromUid(dto.getFromUid());
        m.setType(dto.getType() == null ? Message.TYPE_TEXT : dto.getType());
        m.setContent(dto.getContent());
        m.setExtInfo(dto.getExtInfo() == null ? new HashMap<>() : dto.getExtInfo());
        m.setClientSeq(dto.getClientSeq());
        m.setStatus(Message.STATUS_NORMAL);
        m.setCreateTime(dto.getCreateTime() == null ? LocalDateTime.now() : dto.getCreateTime());

        int inserted = messageMapper.insertIgnoreDuplicate(m);
        if (inserted == 0) {
            log.debug("duplicate message ignored: uid={}, room={}, seq={}",
                    dto.getFromUid(), dto.getRoomId(), dto.getClientSeq());
            return false;
        }
        return true;
    }

    @Override
    public List<Message> listSince(Long roomId, Long sinceId, int size) {
        if (roomId == null || sinceId == null) return java.util.Collections.emptyList();
        // 上限保护：避免前端传 limit=99999 把服务端 OOM
        int capped = Math.min(Math.max(size, 1), 500);
        return messageMapper.listSince(roomId, sinceId, capped);
    }

    @Override
    public void updateLastMsg(Long roomId, Long msgId) {
        contactMapper.updateLastMsg(roomId, msgId);
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

        // 通知房间所有在线成员（统一走 /user/queue/messages）
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "recall");
        evt.put("msgId", msgId);
        evt.put("roomId", m.getRoomId());

        ChatRoom room = chatRoomMapper.selectById(m.getRoomId());
        if (room != null) {
            if (room.getType() == ChatRoom.TYPE_SINGLE) {
                // 单聊：推送给双方
                Map<String, Object> ext = room.getExtInfo();
                if (ext != null) {
                    Long uid1 = Long.parseLong(ext.get("uid1").toString());
                    Long uid2 = Long.parseLong(ext.get("uid2").toString());
                    if (onlineUserService.isOnline(uid1))
                        onlineUserService.push(uid1, "/queue/messages", evt);
                    if (onlineUserService.isOnline(uid2))
                        onlineUserService.push(uid2, "/queue/messages", evt);
                }
            } else {
                // 群聊：推送给所有在线群成员
                QueryWrapper<ChatGroup> gq = new QueryWrapper<ChatGroup>()
                        .eq("room_id", m.getRoomId()).eq("deleted", false);
                ChatGroup group = chatGroupMapper.selectOne(gq);
                if (group != null) {
                    List<Long> members = chatGroupMemberMapper.listMemberUids(group.getId());
                    for (Long memberUid : members) {
                        if (onlineUserService.isOnline(memberUid)) {
                            onlineUserService.push(memberUid, "/queue/messages", evt);
                        }
                    }
                }
            }
        }
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
        badgeService.pushBadgeIfOnline(readerId);
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