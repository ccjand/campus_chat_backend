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

    // ======== 新增的签到类型常量 ========
    public static final int TYPE_LOCATION   = 1; // 定位签到
    public static final int TYPE_CODE       = 2; // 签到码签到
    public static final int TYPE_QR         = 3; // 扫码签到
    public static final int TYPE_SUPPLEMENT = 4; // 补签
    // ===================================

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;
    private Long studentId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer distanceM;
    private Integer status;
    private Integer type; // <--- 新增的签到类型字段
    private LocalDateTime checkinTime;
}