package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对齐论文 3.4：三类用户角色（1=学生 2=教师 3=管理员）
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    public static final int ROLE_STUDENT = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_ADMIN   = 3;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNumber;
    private String passwordHash;
    private String name;
    private Integer role;
    private Long departmentId;
    private String avatar;
    private String phone;
    private String email;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}