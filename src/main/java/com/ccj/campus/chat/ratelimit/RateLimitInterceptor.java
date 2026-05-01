package com.ccj.campus.chat.ratelimit;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.security.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器。对齐论文 4.1 API 网关层：
 * "Spring Security 的过滤器链负责对所有入站请求进行 JWT 令牌验证与角色权限判断"
 * 之外，额外提供基于 Redis 滑动窗口的接口级限流能力。
 * <p>
 * 实现原理：以 "rate:{keyType}:{key}:{path}" 为 Redis key，
 * 每次请求 INCR 计数，首次设置 TTL 等于窗口时长，
 * 计数超过阈值即拒绝并返回 429。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true;
        }

        String key = buildKey(annotation, request);
        int window = annotation.window();
        int max = annotation.maxRequests();

        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            return true;
        }

        // 首次计数时设置过期时间
        if (count == 1) {
            redis.expire(key, window, TimeUnit.SECONDS);
        }

        if (count > max) {
            log.warn("限流触发: key={}, count={}, max={}", key, count, max);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            R<?> result = new R<>();
            result.setCode(429);
            result.setMsg("请求过于频繁，请稍后再试");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return false;
        }

        return true;
    }

    private String buildKey(RateLimit annotation, HttpServletRequest request) {
        String path = request.getRequestURI();
        String identity;

        if ("user".equals(annotation.keyType())) {
            // 已登录接口按 uid 限流
            try {
                identity = String.valueOf(LoginUser.currentUid());
            } catch (Exception e) {
                identity = getClientIp(request);
            }
        } else {
            // 未登录接口按 IP 限流
            identity = getClientIp(request);
        }

        return KEY_PREFIX + annotation.keyType() + ":" + identity + ":" + path;
    }

    /**
     * 获取客户端真实 IP，兼容 Nginx 反向代理场景
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}