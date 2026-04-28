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
 * 【性能优化版】安全配置。
 * <p>
 * BCrypt strength 从默认 10 降到 4：
 * - 只影响新注册/改密码时生成的哈希（$2a$04$...）
 * - 老用户的 $2a$10$ 哈希在 matches() 时仍按 strength=10 校验，完全兼容
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
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .antMatchers("/auth/login", "/auth/register",
                        "/user/updatePassword",
                        "/actuator/**",
                        "/ws/**",
                        "/error").permitAll()
                .antMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
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