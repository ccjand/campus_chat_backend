package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.Contact;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ContactMapper extends BaseMapper<Contact> {
    /** 统计用户所有会话的未读消息总数 */
    int countTotalUnread(@Param("userId") Long userId);

    /** 进入房间时，将 last_read_id 对齐到 last_msg_id */
    int markRoomRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /** 会话列表（带未读数、最后一条消息内容） */
    List<Map<String, Object>> listWithUnread(@Param("userId") Long userId);

}