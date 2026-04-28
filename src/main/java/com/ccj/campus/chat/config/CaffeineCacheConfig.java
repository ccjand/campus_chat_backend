package com.ccj.campus.chat.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ccj.campus.chat.entity.ChatRoom;
import com.ccj.campus.chat.entity.SysUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置。
 * <p>
 * 两级缓存架构：L1 = Caffeine（进程内，纳秒级） → L2 = Redis（网络，毫秒级） → DB
 * <p>
 * 一致性策略：
 * 1. 写操作时主动清除 L1 + L2（write-through eviction）
 * 2. Caffeine 设置短 TTL（2~5分钟）作为兜底，即使漏清也能自愈
 * 3. Redis 设置较长 TTL（10分钟），Caffeine miss 时回填
 */
@Configuration
public class CaffeineCacheConfig {

    /**
     * 好友关系缓存：key = "userId:friendId"（min:max 归一化），value = Boolean
     * 好友关系变更频率极低，缓存 5 分钟足够安全
     */
    @Bean
    public Cache<String, Boolean> friendCache() {
        return Caffeine.newBuilder()
                .maximumSize(50_000)          // 最多缓存 5万 对关系
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 黑名单缓存：key = "userId:targetId"（有方向），value = Boolean
     */
    @Bean
    public Cache<String, Boolean> blockCache() {
        return Caffeine.newBuilder()
                .maximumSize(20_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 房间信息缓存：key = roomId, value = ChatRoom
     * 房间创建后 type 和 extInfo 不变，可以缓存较长时间
     */
    @Bean
    public Cache<Long, ChatRoom> roomCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 发送者信息缓存：key = uid, value = SysUser
     * 用于 enrichSenderProfile，昵称头像变更频率低
     */
    @Bean
    public Cache<Long, SysUser> senderProfileCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }
}