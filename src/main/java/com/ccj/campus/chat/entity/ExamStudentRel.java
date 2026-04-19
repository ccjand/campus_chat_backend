package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 考试-学生关系（联合主键 exam_id + student_id）
 */
@Data
@TableName("exam_student_rel")
public class ExamStudentRel implements Serializable {
    private Long examId;
    private Long studentId;
    private String seatNo;
}