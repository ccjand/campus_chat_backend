package com.ccj.campus.chat.config;

import com.ccj.campus.chat.security.JwtAccessDeniedHandler;
import com.ccj.campus.chat.security.JwtAuthenticationEntryPoint;
import com.ccj.campus.chat.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 对齐论文 3.4 + 4.4：
 * - 过滤器链 + 方法级注解 (@PreAuthorize) 双重保障
 * - JWT 无状态认证，不建 HTTP Session
 * - 认证失败 401 / 越权 403，由专门的 handler 返回
 */
@Configuration
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authEntryPoint;
    private final JwtAccessDeniedHandler deniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 对齐论文 4.4：密码以哈希形式存储
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                // 登录、健康检查、WebSocket 握手端点放行（具体鉴权在 STOMP 拦截器内）
                .antMatchers("/auth/login", "/auth/register",
                        "/user/updatePassword",
                        "/actuator/**",
                        "/ws/**",
                        "/error").permitAll()
                // 管理员专属后台
                .antMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                // 教师专属
                .antMatchers(HttpMethod.POST, "/checkin/teacher/**").hasAnyAuthority("ROLE_TEACHER", "ROLE_ADMIN")
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(deniedHandler)
                .and()
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}