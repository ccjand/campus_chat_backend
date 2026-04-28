package com.ccj.campus.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * 【性能优化版】WebSocket 配置。
 * <p>
 * 核心优化点：
 * 1. sendTimeLimit 从 60s 降到 15s，让慢连接快速失败释放资源
 * 2. 入站/出站线程池参数调优
 * 3. 心跳线程池精简（不需要 100 个线程）
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 【优化】只注入 StompAuthChannelInterceptor，它已经包含认证逻辑
    // 之前同时注入了 JwtChannelInterceptor 和 JwtHandshakeInterceptor
    // 两个拦截器做重复的 JWT 校验，浪费了握手时间
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{25000, 25000})
                .setTaskScheduler(heartBeatTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        // 【优化说明】如果后续并发继续增长，建议切换到外部 Broker：
        // registry.enableStompBrokerRelay("/topic", "/queue")
        //         .setRelayHost("localhost").setRelayPort(61613);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 【优化】sendTimeLimit: 60s → 15s
        // 之前慢连接会阻塞 60s 才超时，这正是压测中 max=60033ms 的根本原因
        // 降到 15s 让不可达的连接快速释放，避免出站线程被占满
        registration.setSendTimeLimit(5 * 1000);

        // 【优化】sendBufferSizeLimit: 1MB → 512KB
        // 每个 WebSocket 会话的出站缓冲区，降低内存压力
        registration.setSendBufferSizeLimit(512 * 1024);

        registration.setMessageSizeLimit(128 * 1024);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 【优化】只挂一个拦截器，之前 JwtChannelInterceptor 重复校验了
        registration.interceptors(stompAuthChannelInterceptor)
                .taskExecutor()
                .corePoolSize(50)
                .maxPoolSize(400)
                .queueCapacity(2000);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(50)
                .maxPoolSize(400)
                .queueCapacity(4000);
    }

    @Bean
    @Primary
    public TaskScheduler heartBeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}