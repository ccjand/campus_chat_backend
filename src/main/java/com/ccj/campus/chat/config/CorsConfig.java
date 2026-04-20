package com.ccj.campus.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * 全局跨域配置。
 * SecurityConfig 中已启用 .cors()，Spring Security 会自动查找
 * 名为 corsConfigurationSource 的 Bean 来获取跨域规则。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();


        // 如果需要允许所有来源（不推荐生产环境使用），可改为：
        config.setAllowedOriginPatterns(List.of("*"));

        // 允许的 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允许的请求头
        config.setAllowedHeaders(List.of("*"));

        // 允许携带凭证（Cookie / Authorization 头）
        config.setAllowCredentials(true);

        // 暴露给前端的响应头
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));

        // 预检请求缓存时间（秒），减少 OPTIONS 请求次数
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}