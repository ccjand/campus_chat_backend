package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.ChatGroupMember;
import com.ccj.campus.chat.mapper.ChatRoomMapper;
import com.ccj.campus.chat.mapper.ChatGroupMemberMapper;
import com.ccj.campus.chat.mapper.ChatGroupMapper;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.ContactService;
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
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomMapper chatRoomMapper;
    private final MessageService messageService;
    private final ContactService contactService;
    private final StringRedisTemplate stringRedisTemplate;

    @MessageMapping("/chat/send")
    public void handleSend(ChatMessageDTO msg, Principal principal) {
        if (principal == null) {
            log.warn("WS 消息无 Principal，丢弃 roomId={}", msg.getRoomId());
            return;
        }
        Long fromUid = Long.valueOf(principal.getName());
        msg.setFromUid(fromUid);
        if (msg.getCreateTime() == null) msg.setCreateTime(LocalDateTime.now());
        if (msg.getId() == null) msg.setId(messageService.prepareMessageId());

        ChatRoom room = chatRoomMapper.selectById(msg.getRoomId());
        if (room == null) {
            log.warn("roomId={} 不存在", msg.getRoomId());
            sendError(fromUid, "房间不存在");
            return;
        }

        // ========== 1. 实时推送 ==========
        if (room.getType() == ChatRoom.TYPE_SINGLE) {
            // 单聊：推给对方
            Long receiverId = msg.getReceiverId();
            if (receiverId == null) receiverId = resolveSingleChatPeer(room, fromUid);
            if (receiverId != null) {
                boolean online = Boolean.TRUE.equals(stringRedisTemplate.hasKey("ws:uid:" + receiverId));
                if (online) {
                    messagingTemplate.convertAndSendToUser(
                            receiverId.toString(), "/queue/messages", msg);
                    log.info("单聊推送 ✅ from={} to={} msgId={}", fromUid, receiverId, msg.getId());
                } else {
                    log.info("单聊接收方离线 from={} to={} msgId={}", fromUid, receiverId, msg.getId());
                    // 如果你有 RocketMQ 离线队列，在这里发：
                    // rocketMQTemplate.syncSend("offline-msg-topic", msg);
                }
            }
            // 回推给发送方自己（用于多端同步 & 把 pending 状态清掉）
            messagingTemplate.convertAndSendToUser(
                    fromUid.toString(), "/queue/messages", msg);

        } else {
            // 群聊：广播到房间 topic，所有订阅该 topic 的成员都会收到
            messagingTemplate.convertAndSend("/topic/room/" + msg.getRoomId(), msg);
            log.info("群聊广播 ✅ room={} from={} msgId={}", msg.getRoomId(), fromUid, msg.getId());
        }

        // ========== 2. 异步持久化 ==========
        try {
            messageService.persist(msg);
        } catch (Exception e) {
            log.error("持久化消息失败 msgId={}", msg.getId(), e);
        }

        // ========== 3. 更新会话最后一条消息 ==========
        try {
            messageService.updateLastMsg(msg.getRoomId(), msg.getId());
        } catch (Exception e) {
            log.warn("更新会话最后消息失败", e);
        }
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
            sendError(uid, "撤回失败：" + e.getMessage());
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

    private void sendError(Long uid, String errMsg) {
        try {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("type", -1);
            err.put("data", errMsg);
            messagingTemplate.convertAndSendToUser(uid.toString(), "/queue/messages", err);
        } catch (Exception ignore) {}
    }
}