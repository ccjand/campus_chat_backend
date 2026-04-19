package com.ccj.campus.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对齐论文 5.4："每次状态变更均记录操作时间和操作人，形成完整的操作日志"
 */
@Data
@TableName("leave_log")
public class LeaveLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long leaveId;
    private Integer fromStatus;
    private Integer toStatus;
    private Long operatorId;
    private String remark;
    private LocalDateTime createTime;
}