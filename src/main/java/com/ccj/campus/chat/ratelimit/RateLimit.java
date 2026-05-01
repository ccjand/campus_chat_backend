package com.ccj.campus.chat.ratelimit;

import java.lang.annotation.*;

/**
 * 接口限流注解。对齐论文 4.1 架构图中 API 网关层的"限流"组件。
 * <p>
 * 基于 Redis 滑动窗口实现，按 IP 或用户维度限制单位时间内的请求次数，
 * 防止恶意刷接口或高频误操作对后端服务造成冲击。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流窗口时长（秒），默认 60 秒 */
    int window() default 60;

    /** 窗口内允许的最大请求次数 */
    int maxRequests() default 20;

    /**
     * 限流维度：
     * "ip"   — 按客户端 IP 限流（适用于未登录接口，如登录）
     * "user" — 按已登录用户 uid 限流（适用于已登录接口，如文件上传）
     */
    String keyType() default "ip";
}