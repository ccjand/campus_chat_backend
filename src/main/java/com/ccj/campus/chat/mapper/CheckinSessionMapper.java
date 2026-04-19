package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.CheckinSession;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CheckinSessionMapper extends BaseMapper<CheckinSession> {

    /** 学生端：我所在班级当前进行中的签到 */
    List<CheckinSession> listActiveForStudent(@Param("studentId") Long studentId);

    /** 查询某次签到覆盖的所有班级 id */
    List<Long> listClassIdsBySession(@Param("sessionId") Long sessionId);

    /** 批量插入签到会话 - 班级关系 */
    int batchInsertSessionClass(@Param("sessionId") Long sessionId,
                                @Param("classIds") List<Long> classIds);
}