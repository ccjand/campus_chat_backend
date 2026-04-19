package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.CheckinRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {

    /** 某次签到的全部记录（教师看） */
    List<CheckinRecord> listBySession(@Param("sessionId") Long sessionId);

    /** 学生-某次签到是否已签 */
    CheckinRecord selectOneByStudent(@Param("sessionId") Long sessionId,
                                      @Param("studentId") Long studentId);
}