package com.ccj.campus.chat.security;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.util.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器。对齐论文 4.4：
 *  - Bearer Token 解析
 *  - Redis 黑名单（登出后主动吊销）
 *  - 成功后注入 Authentication 供 @PreAuthorize 使用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "Authorization";
    public static final String PREFIX = "Bearer ";
    public static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redis;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        String token = extract(request);
        if (StringUtils.isBlank(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 对齐论文：Redis 黑名单实现"主动令牌失效"
            if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_KEY_PREFIX + token))) {
                log.debug("token hit blacklist");
                chain.doFilter(request, response);
                return;
            }
            Claims claims = jwtUtils.parse(token);
            Long uid = Long.valueOf(claims.getSubject());
            Integer role = claims.get("role", Integer.class);

            String authority = roleToAuthority(role);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    new LoginUser(uid, role),
                    null,
                    List.of(new SimpleGrantedAuthority(authority)));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (BusinessException e) {
            // 留给 EntryPoint 处理，这里先清上下文
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private String extract(HttpServletRequest req) {
        String h = req.getHeader(HEADER);
        if (StringUtils.isNotBlank(h) && h.startsWith(PREFIX)) {
            return h.substring(PREFIX.length());
        }
        return null;
    }

    private String roleToAuthority(Integer role) {
        if (role == null) return "ROLE_ANONYMOUS";
        switch (role) {
            case 1: return "ROLE_STUDENT";
            case 2: return "ROLE_TEACHER";
            case 3: return "ROLE_ADMIN";
            default: return "ROLE_ANONYMOUS";
        }
    }
}