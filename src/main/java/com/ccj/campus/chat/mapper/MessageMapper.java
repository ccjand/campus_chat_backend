package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.mybatis.spring.annotation.MapperScan;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息 Mapper。
 * BaseMapper 提供 CRUD；自定义查询由 MessageMapper.xml 实现。
 */
public interface MessageMapper extends BaseMapper<Message> {

    int insertIgnoreDuplicate(Message message);

    Long nextMessageId();

    /**
     * 分页拉取历史消息（按时间倒序，只取可见）
     */
    List<Message> pullHistory(@Param("roomId") Long roomId,
                              @Param("cursor") LocalDateTime cursor,
                              @Param("size") int size);

    /**
     * 撤回（软删除）
     */
    int recall(@Param("id") Long id, @Param("uid") Long uid);

    /**
     * 增量拉取：拉取 id 严格大于 sinceId 的可见消息，按 id 升序。
     * 用于前端断线重连 / 从后台回前台时的 gap 补齐。
     */
    List<Message> listSince(@Param("roomId") Long roomId,
                            @Param("sinceId") Long sinceId,
                            @Param("size") int size);
}