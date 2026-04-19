package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.ExamStudentRel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExamStudentRelMapper extends BaseMapper<ExamStudentRel> {

    /** 批量绑定学生到考试 */
    int batchInsert(@Param("examId") Long examId, @Param("list") List<ExamStudentRel> list);
}