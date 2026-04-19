package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course")
public class Course implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long teacherId;
    private LocalDateTime createTime;
}