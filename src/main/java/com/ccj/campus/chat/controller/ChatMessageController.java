package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.service.MessageService;
import com.ccj.campus.chat.service.OutboxService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * WebSocket 消息路由核心。严格对齐论文 5.2 Listing 1 伪代码：
 *
 *   1. 查 Redis 判断接收方在线
 *   2. 在线 -> convertAndSendToUser 直推
 *   3. 离线 -> RocketMQ 离线消息主题
 *   4. 无论在线离线，异步持久化到 PostgreSQL —— 不阻塞推送链路
 *   5. 更新会话列表最新消息
 *
 * 工程强化：持久化通过"本地消息表 + RocketMQ"实现最终一致性。
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatMessageController {

    private final OnlineUserService onlineUserService;
    private final OutboxService outboxService;
    private final MessageService messageService;

    /** 发送消息。客户端 STOMP send destination = /app/chat/send */
    @MessageMapping("/chat/send")
    public void handleMessage(ChatMessageDTO msg, Principal principal) {
        // 从 STOMP Principal 取真实 uid，禁止客户端伪造
        Long fromUid = Long.parseLong(principal.getName());
        msg.setFromUid(fromUid);
        msg.setCreateTime(LocalDateTime.now());

        Long msgId = messageService.prepareMessageId();
        msg.setId(msgId);

        boolean isOnline = onlineUserService.isOnline(msg.getReceiverId());
        if (isOnline) {
            // 在线: 直接 STOMP 点对点推送
            onlineUserService.push(msg.getReceiverId(), "/queue/messages", msg);
        } else {
            // 离线: 投递到离线消息 topic，待上线补推
            outboxService.enqueue(MQTopic.OFFLINE_MSG_TOPIC, "offline", msg);
        }

        // 无论在线离线，异步持久化（本地消息表 + MQ 保障可靠性）
        // 持久化操作被完全移出推送关键路径
        outboxService.enqueue(MQTopic.MSG_PERSIST_TOPIC, "persist", msg);

        // 更新会话列表最新消息
        messageService.updateLastMsg(msg.getRoomId(), msgId);
    }

    /** 撤回。对齐论文 3.2：2 分钟内可撤回。 */
    @MessageMapping("/chat/recall")
    public void handleRecall(Long msgId, Principal principal) {
        Long uid = Long.parseLong(principal.getName());
        messageService.recall(msgId, uid);
    }

    /** 已读回执。对齐论文 3.2：基于数据库时间戳的已读回执。 */
    @MessageMapping("/chat/read")
    public void handleRead(ChatMessageDTO msg, Principal principal) {
        Long uid = Long.parseLong(principal.getName());
        messageService.markRead(msg.getRoomId(), msg.getId(), uid);
    }
}