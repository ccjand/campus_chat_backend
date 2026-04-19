package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.MessageOutbox;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    /** 扫描待发送的消息 */
    List<MessageOutbox> scanPending(@Param("now") LocalDateTime now,
                                     @Param("limit") int limit);
}