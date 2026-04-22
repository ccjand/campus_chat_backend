package com.ccj.campus.chat.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生端「我的签到记录」按课程聚合的外层 VO。
 * 与前端 src/pages/workbench/checkin/index.vue 的 studentHistoryCourses 结构对齐。
 */
@Data
public class StudentHistoryCourseVO implements Serializable {
    private Long courseId;
    private String courseName;
    private List<StudentHistoryRecordVO> records = new ArrayList<>();
}