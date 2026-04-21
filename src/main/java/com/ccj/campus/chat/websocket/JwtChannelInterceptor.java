package com.ccj.campus.chat.websocket;

import com.ccj.campus.chat.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;
        StompCommand cmd = accessor.getCommand();

        if (StompCommand.CONNECT.equals(cmd)) {
            String token = extractToken(accessor);
            if (token == null) {
                log.warn("STOMP CONNECT 缺少 token，拒绝");
                throw new IllegalArgumentException("Missing Authorization token");
            }
            Long uid;
            try {
                uid = jwtUtils.getUid(token); // ← 换成你项目里的方法
            } catch (Exception e) {
                log.warn("STOMP CONNECT token 无效：{}", e.getMessage());
                throw new IllegalArgumentException("Invalid token");
            }
            // 关键：设置 Principal，name 为 uid 字符串。
            // 这样 convertAndSendToUser(uidStr, "/queue/messages", ...) 才能路由到这条会话
            final String uidStr = String.valueOf(uid);
            Principal principal = () -> uidStr;
            accessor.setUser(principal);

            // 在线状态写 Redis，30 分钟 TTL，心跳时续期
            stringRedisTemplate.opsForValue().set(
                    "ws:uid:" + uid, accessor.getSessionId(), 30, TimeUnit.MINUTES);
            log.info("STOMP CONNECT ✅ uid={} sessionId={}", uid, accessor.getSessionId());

        } else if (StompCommand.SEND.equals(cmd) || StompCommand.SUBSCRIBE.equals(cmd)) {
            Principal p = accessor.getUser();
            if (p != null) {
                stringRedisTemplate.expire("ws:uid:" + p.getName(), 30, TimeUnit.MINUTES);
            }
        } else if (StompCommand.DISCONNECT.equals(cmd)) {
            Principal p = accessor.getUser();
            if (p != null) {
                stringRedisTemplate.delete("ws:uid:" + p.getName());
                log.info("STOMP DISCONNECT uid={}", p.getName());
            }
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1) 从 STOMP 原生头取 Authorization
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        if (auth != null && !auth.isEmpty()) {
            return auth;
        }
        // 2) 兜底：从握手阶段写入的 session 属性里取
        if (accessor.getSessionAttributes() != null) {
            Object t = accessor.getSessionAttributes().get("token");
            if (t != null) {
                String s = t.toString();
                return s.startsWith("Bearer ") ? s.substring(7) : s;
            }
        }
        return null;
    }
}