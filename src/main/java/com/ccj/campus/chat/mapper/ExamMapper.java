package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.Exam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExamMapper extends BaseMapper<Exam> {

    /** 学生查询自己的考试安排 */
    List<Exam> listByStudent(@Param("studentId") Long studentId);
}