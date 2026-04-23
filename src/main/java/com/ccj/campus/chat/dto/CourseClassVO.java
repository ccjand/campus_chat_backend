package com.ccj.campus.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 课程关联班级列表项。对齐前端 workbench/checkin/index.vue：
 *   teacherClasses.value.map((c) => c.classId)
 *   teacherClassNameMap[cid]  （classId -> className）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseClassVO implements Serializable {
    private Long classId;
    private String className;
    /** 年级，可选展示用 */
    private Integer grade;
}