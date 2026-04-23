package com.ccj.campus.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 老师端"待审批补签申请"列表项。
 * <p>
 * 对齐好友申请 FriendRequestVO 的设计：后端一次性把列表渲染需要的所有字段
 * （发起人名称、头像、学号、对应签到的课程名与标题）拼好，避免前端 N+1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSupplementVO implements Serializable {

    /**
     * 补签记录 id，审批时带回
     */
    private Long supplementId;

    private Long id;              // 对应补签记录的 id
    private String studentNo;     // 对应学号
    private String approveNote;   // 审批意见

    /**
     * 关联的签到会话 id
     */
    private Long sessionId;

    /**
     * 申请人（学生）基础信息
     */
    private Long studentId;
    private String studentName;
    private String studentAccountNumber;
    private String studentAvatar;

    /**
     * 签到会话所属的课程与标题，便于老师辨认
     */
    private Long courseId;
    private String courseName;
    private String sessionTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sessionStartTime;

    /**
     * 补签理由
     */
    private String reason;

    /**
     * 0=待审批 1=已同意 2=已拒绝
     */
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}