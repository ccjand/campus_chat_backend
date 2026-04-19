package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到记录。对齐论文 5.3：session_id + student_id 联合唯一约束杜绝重复签到。
 */
@Data
@TableName("checkin_record")
public class CheckinRecord implements Serializable {

    public static final int STATUS_NORMAL       = 1;
    public static final int STATUS_LATE         = 2;
    public static final int STATUS_OUT_RANGE    = 3;
    public static final int STATUS_ABNORMAL     = 4;
    public static final int STATUS_SUPPLEMENTED = 5;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long studentId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer distanceM;
    private Integer status;
    private LocalDateTime checkinTime;
}