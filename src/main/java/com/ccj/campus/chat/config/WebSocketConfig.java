package com.ccj.campus.chat.config;

import com.ccj.campus.chat.websocket.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 对齐论文 5.2：
 * - 以 Spring WebSocket 框架为基础，结合 STOMP 协议构建消息代理
 * - 单聊走 /user/queue/..., 群聊走 /topic/...
 * - 握手阶段拦截 JWT 令牌，通过校验才允许建立连接
 * - 30 秒心跳（STOMP 层内置，serverHeartbeat/clientHeartbeat）
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 对齐论文 5.2: 主握手端点
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();   // 降级支持，兼顾 H5

        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*");  // 原生 WebSocket 端点
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 客户端发消息的前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 单聊：messagingTemplate.convertAndSendToUser(uid, "/queue/messages", msg)
        // 群聊：messagingTemplate.convertAndSend("/topic/room/{roomId}", msg)
        registry.enableSimpleBroker("/queue", "/topic")
                // 论文 5.2: 心跳保活机制以 30 秒为周期
                .setHeartbeatValue(new long[]{30_000, 30_000})
                .setTaskScheduler(new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler() {{
                    setPoolSize(2);
                    setThreadNamePrefix("ws-heartbeat-");
                    initialize();
                }});

        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 入站通道加 JWT 认证拦截器
        registration.interceptors(authInterceptor);
    }
}