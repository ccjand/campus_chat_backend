package com.ccj.campus.chat.mq;

import com.ccj.campus.chat.mq.MQTopic;
import com.ccj.campus.chat.websocket.OnlineUserService;
import com.ccj.campus.chat.dto.ChatMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 离线消息消费者。对齐论文 5.2：
 * "若接收方不在线，则将消息发布至 RocketMQ 的离线消息主题，
 *  待接收方上线建立连接后触发离线消息补推逻辑。"
 *
 * 实际补推由 WebSocket CONNECT 事件触发 pullOfflineMessages()，
 * 这里的消费者仅负责把"待推"的消息写入用户的离线队列（Redis ZSET）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQTopic.OFFLINE_MSG_TOPIC,
        consumerGroup = "campus_chat_offline_group"
)
public class OfflineMessageConsumer implements RocketMQListener<String> {

    private final ObjectMapper mapper;
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private final OnlineUserService onlineUserService;

    public static final String OFFLINE_QUEUE_PREFIX = "offline:queue:";

    @Override
    public void onMessage(String json) {
        try {
            ChatMessageDTO msg = mapper.readValue(json, ChatMessageDTO.class);

            // 如果用户已经在线（比如在 MQ 投递期间上线了），直接补推一次
            if (onlineUserService.isOnline(msg.getReceiverId())) {
                onlineUserService.push(msg.getReceiverId(), "/queue/messages", msg);
                return;
            }

            // 否则进 Redis ZSET 排队，上线时客户端/服务端拉取
            String key = OFFLINE_QUEUE_PREFIX + msg.getReceiverId();
            double score = msg.getCreateTime() == null
                    ? System.currentTimeMillis()
                    : msg.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            redisTemplate.opsForZSet().add(key, json, score);
            redisTemplate.expire(key, java.time.Duration.ofDays(7));
        } catch (Exception e) {
            log.error("offline consumer error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}