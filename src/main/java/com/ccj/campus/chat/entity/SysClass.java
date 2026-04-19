package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_class")
public class SysClass implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long departmentId;
    private String name;
    private Integer grade;
    private LocalDateTime createTime;
}