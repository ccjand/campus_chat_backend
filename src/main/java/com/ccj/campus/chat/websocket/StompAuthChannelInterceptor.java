package com.ccj.campus.chat.websocket;

import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * STOMP 通道拦截器。对齐论文 5.2：
 *  - CONNECT 帧内携带的 Authorization: Bearer <jwt>
 *  - 校验通过后，将用户信息写入 Principal；用于 convertAndSendToUser 路由
 *  - Redis 记录在线状态：ws:uid:{uid} = sessionId，TTL = 心跳周期 * 2
 *  - DISCONNECT 时清理 Redis 在线映射
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String ONLINE_KEY_PREFIX = "ws:uid:";
    public static final Duration ONLINE_TTL = Duration.ofSeconds(90);

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redis;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand cmd = accessor.getCommand();
        if (cmd == null) return message;

        switch (cmd) {
            case CONNECT:
                handleConnect(accessor);
                break;
            case SUBSCRIBE:
            case SEND:
                // 二次保险：非 CONNECT 帧也要确保 user 已经在
                ensureAuthenticated(accessor);
                break;
            case DISCONNECT:
                handleDisconnect(accessor);
                break;
            default:
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        List<String> auths = accessor.getNativeHeader("Authorization");
        if (auths == null || auths.isEmpty()) {
            throw new IllegalArgumentException("missing Authorization");
        }
        String bearer = auths.get(0);
        String token = StringUtils.removeStart(bearer, "Bearer ");

        Claims claims = jwtUtils.parse(token);
        Long uid = Long.valueOf(claims.getSubject());
        Integer role = claims.get("role", Integer.class);

        LoginUser lu = new LoginUser(uid, role);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                lu, null,
                List.of(new SimpleGrantedAuthority(authOf(role))));

        // 用 uid 作为 STOMP 的 user name，后续 convertAndSendToUser(uid,...) 就能路由
        accessor.setUser(() -> String.valueOf(uid));
        accessor.setLeaveMutable(true);
        // Spring Security 需要 Authentication 而不是 Principal 本身
        accessor.getSessionAttributes().put("AUTH", auth);
        accessor.getSessionAttributes().put("UID", uid);

        // 对齐论文 5.2: Redis 写入在线映射
        String sessionId = accessor.getSessionId();
        redis.opsForValue().set(ONLINE_KEY_PREFIX + uid, sessionId, ONLINE_TTL);
        log.info("ws connect: uid={}, sessionId={}", uid, sessionId);
    }

    private void ensureAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Object uid = accessor.getSessionAttributes().get("UID");
            if (uid != null) {
                accessor.setUser(() -> String.valueOf(uid));
            }
        }
        // 每次发包续期，实现论文"心跳 + 缓存续期一体化"
        if (accessor.getSessionAttributes() != null) {
            Object uid = accessor.getSessionAttributes().get("UID");
            if (uid != null) {
                redis.expire(ONLINE_KEY_PREFIX + uid, ONLINE_TTL);
            }
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) return;
        Object uid = accessor.getSessionAttributes().get("UID");
        if (uid != null) {
            redis.delete(ONLINE_KEY_PREFIX + uid);
            log.info("ws disconnect: uid={}", uid);
        }
    }

    private String authOf(Integer role) {
        if (role == null) return "ROLE_ANONYMOUS";
        switch (role) {
            case 1: return "ROLE_STUDENT";
            case 2: return "ROLE_TEACHER";
            case 3: return "ROLE_ADMIN";
            default: return "ROLE_ANONYMOUS";
        }
    }
}