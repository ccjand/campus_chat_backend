package com.ccj.campus.chat.service;

import com.ccj.campus.chat.entity.LeaveApplication;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假审批业务接口。对齐论文 5.4 请假审批工作流：
 *   轻量级状态机：0 待审批 → 1 已通过 / 2 已驳回 / 3 已撤销
 */
public interface LeaveService {

    /** 学生：提交请假申请 */
    LeaveApplication apply(Long applicantId, Long approverId, int type,
                            String reason, LocalDateTime startTime, LocalDateTime endTime);

    /** 审批人：通过 */
    void approve(Long approverId, Long leaveId, String note);

    /** 审批人：驳回 */
    void reject(Long approverId, Long leaveId, String note);

    /** 学生：撤回申请（仅限审批完成前） */
    void revoke(Long applicantId, Long leaveId);

    /** 审批人：待审批列表 */
    List<LeaveApplication> listPending(Long approverId);

    /** 学生：自己的请假历史 */
    List<LeaveApplication> listMine(Long applicantId);
}