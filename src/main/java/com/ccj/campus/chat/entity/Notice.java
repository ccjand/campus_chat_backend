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
 * 通知。对齐论文 3.2 通知与考试模块：
 *   scope_type: 1=全量  2=按院系  3=按班级  4=按个人
 *   scope_data: JSONB 存定向推送目标
 */
@Data
@TableName(value = "notice", autoResultMap = true)
public class Notice implements Serializable {

    public static final int SCOPE_ALL        = 1;
    public static final int SCOPE_DEPARTMENT = 2;
    public static final int SCOPE_CLASS      = 3;
    public static final int SCOPE_PERSONAL   = 4;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private Long publisherId;
    private Integer scopeType;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> scopeData;

    private LocalDateTime createTime;
}