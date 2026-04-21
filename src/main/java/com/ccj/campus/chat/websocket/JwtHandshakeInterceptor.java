package com.ccj.campus.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WS 握手阶段：把 URL query 里的 token 写入 session attributes，
 * 作为 STOMP CONNECT 帧取不到 Authorization 时的兜底。
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            String token = ((ServletServerHttpRequest) request)
                    .getServletRequest().getParameter("token");
            if (token != null && !token.isEmpty()) {
                attributes.put("token", token);
                log.debug("WS 握手阶段拿到 token，长度={}", token.length());
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) log.warn("WS 握手后异常", exception);
    }
}