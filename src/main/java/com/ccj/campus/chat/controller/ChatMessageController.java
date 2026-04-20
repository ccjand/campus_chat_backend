package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.BadgeService;
import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.service.OutboxService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final OnlineUserService onlineUserService;
    private final OutboxService outboxService;
    private final MessageService messageService;
    private final BadgeService badgeService;

    @Transactional
    @MessageMapping("/chat/send")
    public void handleMessage(ChatMessageDTO msg, Principal principal) {
        Long fromUid = Long.parseLong(principal.getName());
        msg.setFromUid(fromUid);
        msg.setCreateTime(LocalDateTime.now());

        Long msgId = messageService.prepareMessageId();
        msg.setId(msgId);

        // ===== 改为同步持久化，确保消息立即入库 =====
        messageService.persist(msg);

        // 更新会话列表最新消息
        messageService.updateLastMsg(msg.getRoomId(), msgId);

        // 推送（持久化完成后再推，保证对方收到时数据库已有记录）
        boolean isOnline = onlineUserService.isOnline(msg.getReceiverId());
        if (isOnline) {
            onlineUserService.push(msg.getReceiverId(), "/queue/messages", msg);
        } else {
            outboxService.enqueue(MQTopic.OFFLINE_MSG_TOPIC, "offline", msg);
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