package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 补签申请。对齐论文 3.2：
 * "对于因特殊原因未能参与签到的学生，可发起补签申请，由教师在线审核。"
 */
@Data
@TableName(value = "checkin_supplement", autoResultMap = true)
public class CheckinSupplement implements Serializable {

    public static final int STATUS_PENDING  = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long studentId;
    private String reason;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> attachment;

    private Integer status;
    private Long approverId;
    private LocalDateTime approveTime;
    private String approveNote;
    private LocalDateTime createTime;
}