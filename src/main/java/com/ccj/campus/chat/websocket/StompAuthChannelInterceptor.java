package com.ccj.campus.chat.websocket;

import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 【性能优化版】STOMP 通道拦截器。
 * <p>
 * 优化点：
 * 1. CONNECT 阶段只做 JWT 校验 + Redis 写在线状态，移除离线消息补推
 * → 离线消息改由客户端连接成功后主动调用 GET /message/offline 拉取
 * 2. SEND/SUBSCRIBE 的 Redis 续期改为节流模式（30秒内只调一次），减少 Redis QPS
 * 3. 移除 MessageService、OnlineUserService、TaskScheduler 依赖，握手路径极轻
 */
@Slf4j
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String ONLINE_KEY_PREFIX = "ws:uid:";
    public static final Duration ONLINE_TTL = Duration.ofSeconds(90);

    /**
     * 续期最小间隔（毫秒），90s TTL 下 30s 续一次足够
     */
    private static final long RENEW_INTERVAL_MS = 30_000;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redis;

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
                ensureAuthenticated(accessor);
                break;
            case DISCONNECT:
                handleDisconnect(accessor);
                break;
            default:
        }
        return message;
    }

    /**
     * CONNECT 只做三件事：解析 JWT → 设置 Principal → Redis 写在线状态。
     * 离线消息补推已移除，改由客户端主动调用 GET /message/offline 拉取。
     */
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

        accessor.setUser(() -> String.valueOf(uid));
        accessor.setLeaveMutable(true);
        accessor.getSessionAttributes().put("AUTH", auth);
        accessor.getSessionAttributes().put("UID", uid);
        accessor.getSessionAttributes().put("LAST_RENEW", System.currentTimeMillis());

        String sessionId = accessor.getSessionId();
        redis.opsForValue().set(ONLINE_KEY_PREFIX + uid, sessionId, ONLINE_TTL);
        log.info("ws connect: uid={}, sessionId={}", uid, sessionId);
    }

    /**
     * 节流续期：距离上次续期不足 30s 的帧直接跳过 redis.expire()。
     */
    private void ensureAuthenticated(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Object uid = accessor.getSessionAttributes().get("UID");
            if (uid != null) {
                accessor.setUser(() -> String.valueOf(uid));
            }
        }

        if (accessor.getSessionAttributes() != null) {
            Object uid = accessor.getSessionAttributes().get("UID");
            if (uid != null) {
                Long lastRenew = (Long) accessor.getSessionAttributes().get("LAST_RENEW");
                long now = System.currentTimeMillis();
                if (lastRenew == null || (now - lastRenew) > RENEW_INTERVAL_MS) {
                    redis.expire(ONLINE_KEY_PREFIX + uid, ONLINE_TTL);
                    accessor.getSessionAttributes().put("LAST_RENEW", now);
                }
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
            case 0:
                return "ROLE_ADMIN";
            case 1:
                return "ROLE_STUDENT";
            case 2:
                return "ROLE_TEACHER";
            case 3:
                return "ROLE_COUNSELOR";
            case 4:
                return "ROLE_STAFF";
            default:
                return "ROLE_ANONYMOUS";
        }
    }
}