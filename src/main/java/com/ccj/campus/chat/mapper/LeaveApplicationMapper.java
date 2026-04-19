package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.LeaveApplication;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LeaveApplicationMapper extends BaseMapper<LeaveApplication> {

    /** 审批人的待审批列表 */
    List<LeaveApplication> listPendingByApprover(@Param("approverId") Long approverId);

    /** 学生的请假历史 */
    List<LeaveApplication> listByApplicant(@Param("applicantId") Long applicantId);
}