package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.dto.StudentCheckinHistoryRow;
import com.ccj.campus.chat.entity.CheckinRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {

    /** 某次签到的全部记录（教师看） */
    List<CheckinRecord> listBySession(@Param("sessionId") Long sessionId);

    /** 学生-某次签到是否已签 */
    CheckinRecord selectOneByStudent(@Param("sessionId") Long sessionId,
                                      @Param("studentId") Long studentId);

    /**
     * 学生历史签到：列出该学生所在班级的所有签到会话（不论是否已签），
     * 关联课程名、以及该学生在对应会话里的签到记录（未签为 null）。
     * 返回按 start_time DESC 排序，最多 200 条。
     */
    List<StudentCheckinHistoryRow> listHistoryByStudent(@Param("studentId") Long studentId);
}