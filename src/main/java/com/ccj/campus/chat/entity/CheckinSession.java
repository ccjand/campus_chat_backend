package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到会话。对齐论文 4.2 + 5.3：
 *  - 高精度经纬度数值类型
 *  - end_time 由 GENERATED ALWAYS AS 计算列自动维护（只读）
 */
@Data
@TableName("checkin_session")
public class CheckinSession implements Serializable {

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_ENDED  = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;
    private Long creatorId;
    private String title;

    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private Integer radiusMeters;
    private Integer durationMinutes;

    private LocalDateTime startTime;

    /** DB 生成的计算列，只读 */
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime endTime;

    private String code;
    private Integer status;
    private LocalDateTime createTime;
}