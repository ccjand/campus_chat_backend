package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.NoticeRead;
import org.apache.ibatis.annotations.Param;

public interface NoticeReadMapper extends BaseMapper<NoticeRead> {

    /** 插入已读记录（忽略重复） */
    int insertIgnore(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    /** 统计某通知的已读人数 */
    int countByNotice(@Param("noticeId") Long noticeId);
}