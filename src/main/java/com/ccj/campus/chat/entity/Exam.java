package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String name;
    private LocalDateTime examTime;
    private Integer durationMinutes;
    private String location;
    private LocalDateTime createTime;
}