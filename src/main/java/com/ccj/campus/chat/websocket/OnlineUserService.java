package com.ccj.campus.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 在线状态 + 单端推送服务。
 * 对齐论文 4.1：独立的"用户长连接管理"业务服务。
 */
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private final StringRedisTemplate redis;
    private final SimpMessagingTemplate messagingTemplate;


    /** 接收方是否在线（对齐论文 5.2 伪代码第一步） */
    public boolean isOnline(Long uid) {
        return redis.hasKey(StompAuthChannelInterceptor.ONLINE_KEY_PREFIX + uid);
    }

    /** 批量在线判断 */
    public List<Long> filterOnline(List<Long> uids) {
        return uids.stream().filter(this::isOnline).collect(Collectors.toList());
    }

    /**
     * 推送消息给指定用户（封装 convertAndSendToUser）。
     * 调用方无需了解 STOMP 细节。
     */
    public void push(Long uid, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(uid), destination, payload);
    }

    /** 群发到某个 topic（如群聊房间） */
    public void broadcast(String topic, Object payload) {
        messagingTemplate.convertAndSend(topic, payload);
    }
}