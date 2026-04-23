package com.ccj.campus.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 老师课程下拉列表项。对齐前端 workbench/checkin/index.vue：
 *   teacherCourses.value.find((c) => ... c.courseName ... c.courseId)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseVO implements Serializable {
    private Long courseId;
    private String courseName;
}