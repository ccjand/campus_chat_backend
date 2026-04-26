package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.ChatGroupMember;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.mapper.ChatRoomMapper;
import com.ccj.campus.chat.mapper.ChatGroupMemberMapper;
import com.ccj.campus.chat.mapper.ChatGroupMapper;
import com.ccj.campus.chat.mapper.SysUserMapper;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.ContactService;
import com.ccj.campus.chat.service.FriendService;
import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.service.OutboxService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMapper chatRoomMapper;
    private final MessageService messageService;
    private final ContactService contactService;
    private final StringRedisTemplate stringRedisTemplate;
    private final FriendService friendService;
    private final SysUserMapper sysUserMapper;
    private final OutboxService outboxService;


    @MessageMapping("/chat/send")
    public void handleSend(ChatMessageDTO msg, Principal principal) {
        if (msg == null) {
            log.warn("WS 消息体为空，丢弃");
            return;
        }
        if (principal == null) {
            log.warn("WS 消息无 Principal，丢弃 roomId={}", msg.getRoomId());
            return;
        }

        Long fromUid = Long.valueOf(principal.getName());
        msg.setFromUid(fromUid);

        // 统一补齐 extInfo.senderName / senderAvatar
        enrichSenderProfile(msg, fromUid);

        if (msg.getCreateTime() == null) msg.setCreateTime(LocalDateTime.now());
        if (msg.getId() == null) msg.setId(messageService.prepareMessageId());

        ChatRoom room = chatRoomMapper.selectById(msg.getRoomId());
        if (room == null) {
            log.warn("roomId={} 不存在", msg.getRoomId());
            sendError(fromUid, "ROOM_NOT_FOUND", "房间不存在", msg.getClientSeq(), msg.getRoomId());
            return;
        }

        if (room.getType() == ChatRoom.TYPE_SINGLE) {
            Long receiverId = msg.getReceiverId();
            if (receiverId == null) receiverId = resolveSingleChatPeer(room, fromUid);
            if (receiverId == null) {
                sendError(fromUid, "RECEIVER_NOT_FOUND", "接收方不存在", msg.getClientSeq(), msg.getRoomId());
                return;
            }

            // 关键：回写 receiverId，离线入队依赖这个字段
            msg.setReceiverId(receiverId);

            // 非好友或任一方拉黑，禁止发送
            boolean isFriend = friendService.isFriend(fromUid, receiverId);
            boolean blockedEitherSide =
                    friendService.isBlocked(fromUid, receiverId) || friendService.isBlocked(receiverId, fromUid);

            if (!isFriend || blockedEitherSide) {
                sendError(fromUid, "NON_FRIEND_RELATION", "非好友关系，无法发送消息", msg.getClientSeq(), msg.getRoomId());
                return;
            }

            boolean online = Boolean.TRUE.equals(stringRedisTemplate.hasKey("ws:uid:" + receiverId));
            if (online) {
                messagingTemplate.convertAndSendToUser(receiverId.toString(), "/queue/messages", msg);
                log.info("单聊推送 ✅ from={} to={} msgId={}", fromUid, receiverId, msg.getId());
            } else {
                // 关键：接收方离线时入离线队列
                try {
                    outboxService.enqueue(MQTopic.OFFLINE_MSG_TOPIC, "offline", msg);
                    log.info("单聊离线入队 ✅ from={} to={} msgId={}", fromUid, receiverId, msg.getId());
                } catch (Exception e) {
                    log.error("单聊离线入队失败 from={} to={} msgId={}", fromUid, receiverId, msg.getId(), e);
                }
            }

            // 发送方回显
            messagingTemplate.convertAndSendToUser(fromUid.toString(), "/queue/messages", msg);

        } else {
            // 群聊广播
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), msg);
            log.info("群聊广播 ✅ room={} from={} msgId={}", msg.getRoomId(), fromUid, msg.getId());
        }

        boolean inserted = false;
        try {
            inserted = messageService.persist(msg);
        } catch (Exception e) {
            log.error("持久化消息失败 msgId={}", msg.getId(), e);
        }

        if (inserted) {
            try {
                messageService.updateLastMsg(msg.getRoomId(), msg.getId());
            } catch (Exception e) {
                log.warn("更新会话最后消息失败", e);
            }
        }
    }

    private void enrichSenderProfile(ChatMessageDTO msg, Long fromUid) {
        Map<String, Object> ext = msg.getExtInfo();
        if (ext == null) ext = new HashMap<>();

        SysUser from = sysUserMapper.selectById(fromUid);
        if (from != null) {
            Object sn = ext.get("senderName");
            if (sn == null || String.valueOf(sn).trim().isEmpty()) {
                ext.put("senderName", from.getName());
            }

            Object sa = ext.get("senderAvatar");
            if (sa == null || String.valueOf(sa).trim().isEmpty()) {
                ext.put("senderAvatar", from.getAvatar());
            }
        }

        msg.setExtInfo(ext);
    }

    @MessageMapping("/chat/recall")
    public void handleRecall(Long msgId, Principal principal) {
        if (principal == null || msgId == null) return;
        Long uid = Long.valueOf(principal.getName());
        try {
            messageService.recall(msgId, uid);
            // 如果需要通知房间所有成员，可以再广播一个事件到 /topic/room/{roomId}
        } catch (Exception e) {
            log.warn("撤回失败 msgId={} uid={}", msgId, uid, e);
            sendError(uid, "500", "撤回失败", null, null);
        }
    }

    private Long resolveSingleChatPeer(ChatRoom room, Long selfUid) {
        if (room.getExtInfo() == null) return null;
        Object uid1 = room.getExtInfo().get("uid1");
        Object uid2 = room.getExtInfo().get("uid2");
        if (uid1 == null || uid2 == null) return null;
        long u1 = Long.parseLong(uid1.toString());
        long u2 = Long.parseLong(uid2.toString());
        return u1 == selfUid ? u2 : u1;
    }

    private void sendError(Long uid, String code, String errMsg, String clientSeq, Long roomId) {
        try {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("type", -1);
            err.put("code", code);
            err.put("data", errMsg);
            err.put("clientSeq", clientSeq);
            err.put("roomId", roomId);
            messagingTemplate.convertAndSendToUser(uid.toString(), "/queue/messages", err);
        } catch (Exception ignore) {
        }
    }
}