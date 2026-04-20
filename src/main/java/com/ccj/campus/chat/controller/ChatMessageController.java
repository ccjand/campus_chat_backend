package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.ChatGroupMember;
import com.ccj.campus.chat.mapper.ChatRoomMapper;
import com.ccj.campus.chat.mapper.ChatGroupMemberMapper;
import com.ccj.campus.chat.mapper.ChatGroupMapper;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.service.OutboxService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
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

    private final OnlineUserService onlineUserService;
    private final OutboxService outboxService;
    private final MessageService messageService;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatGroupMapper chatGroupMapper;
    private final ChatGroupMemberMapper chatGroupMemberMapper;

    @Transactional
    @MessageMapping("/chat/send")
    public void handleMessage(ChatMessageDTO msg, Principal principal) {
        Long fromUid = Long.parseLong(principal.getName());
        msg.setFromUid(fromUid);
        msg.setCreateTime(LocalDateTime.now());

        Long msgId = messageService.prepareMessageId();
        msg.setId(msgId);

        // 同步持久化
        messageService.persist(msg);

        // 更新会话列表最新消息
        messageService.updateLastMsg(msg.getRoomId(), msgId);

        // 查出房间所有接收方，逐个推送
        List<Long> receivers = resolveReceivers(msg.getRoomId(), fromUid);
        for (Long uid : receivers) {
            if (onlineUserService.isOnline(uid)) {
                // 在线：通过 WebSocket 推送到个人队列
                onlineUserService.push(uid, "/queue/messages", msg);
            } else {
                // 离线：入队等上线补推
                outboxService.enqueue(MQTopic.OFFLINE_MSG_TOPIC, "offline", msg);
            }
        }
    }

    /**
     * 根据房间类型解析所有接收方（排除发送者自己）
     */
    private List<Long> resolveReceivers(Long roomId, Long fromUid) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null) return List.of();

        List<Long> receivers = new ArrayList<>();

        if (room.getType() == ChatRoom.TYPE_SINGLE) {
            // 单聊：从 ext_info 取 uid1、uid2，排除发送方
            Long u1 = toLong(room.getExtInfo().get("uid1"));
            Long u2 = toLong(room.getExtInfo().get("uid2"));
            if (u1 != null && !u1.equals(fromUid)) receivers.add(u1);
            if (u2 != null && !u2.equals(fromUid)) receivers.add(u2);
        } else if (room.getType() == ChatRoom.TYPE_GROUP) {
            // 群聊：查群成员表，排除发送方
            // 先通过 room_id 找到 group_id
            var group = chatGroupMapper.selectOne(
                    new QueryWrapper<com.ccj.campus.chat.entity.ChatGroup>()
                            .eq("room_id", roomId));
            if (group != null) {
                List<ChatGroupMember> members = chatGroupMemberMapper.selectList(
                        new QueryWrapper<ChatGroupMember>().eq("group_id", group.getId()));
                for (ChatGroupMember m : members) {
                    if (!m.getUserId().equals(fromUid)) {
                        receivers.add(m.getUserId());
                    }
                }
            }
        }
        return receivers;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @MessageMapping("/chat/recall")
    public void handleRecall(Long msgId, Principal principal) {
        Long uid = Long.parseLong(principal.getName());
        messageService.recall(msgId, uid);
    }

    @MessageMapping("/chat/read")
    public void handleRead(ChatMessageDTO msg, Principal principal) {
        Long uid = Long.parseLong(principal.getName());
        messageService.markRead(msg.getRoomId(), msg.getId(), uid);
    }
}