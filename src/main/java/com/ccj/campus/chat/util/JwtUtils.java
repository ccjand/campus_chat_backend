package com.ccj.campus.chat.util;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expire-days}")
    private int expireDays;

    @Getter
    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成令牌。对齐论文 3.2：角色信息在令牌签发时写入载荷。
     */
    public String createToken(Long uid, Integer role) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + TimeUnit.DAYS.toMillis(expireDays));
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(String.valueOf(uid))
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            log.warn("JWT parse failed: {}", e.getMessage());
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
    }

    public Long getUid(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public Integer getRole(String token) {
        return parse(token).get("role", Integer.class);
    }
}