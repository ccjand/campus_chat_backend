package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 请假申请。对齐论文 5.4 状态机：
 *   0=待审批  1=已通过  2=已驳回  3=已撤销
 * 每次状态变更均记录操作时间和操作人。
 */
@Data
@TableName(value = "leave_application", autoResultMap = true)
public class LeaveApplication implements Serializable {

    public static final int STATUS_PENDING  = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_REVOKED  = 3;

    public static final int TYPE_SICK   = 1;
    public static final int TYPE_EVENT  = 2;
    public static final int TYPE_OTHER  = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applicantId;
    private Long approverId;
    private Integer type;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> attachments;

    private Integer status;
    private String approveNote;
    private LocalDateTime approveTime;
    private Long cardMsgId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}