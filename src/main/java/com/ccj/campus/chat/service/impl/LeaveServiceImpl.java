package com.ccj.campus.chat.service.impl;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.entity.LeaveApplication;
import com.ccj.campus.chat.entity.LeaveLog;
import com.ccj.campus.chat.mapper.LeaveApplicationMapper;
import com.ccj.campus.chat.mapper.LeaveLogMapper;
import com.ccj.campus.chat.service.LeaveService;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 请假审批实现。对齐论文 5.4：
 *
 * 状态机流转：
 *   0(待审批) → 1(已通过)   由审批人操作
 *   0(待审批) → 2(已驳回)   由审批人操作，需填写驳回意见
 *   0(待审批) → 3(已撤销)   由申请人操作
 *
 * 交互：
 *   "学生提交请假申请后，服务端通过 WebSocket 向对应教师推送请假卡片。"
 *   "审批人可直接在聊天界面内嵌的请假卡片上完成审批操作。"
 *   "审批结果即时反馈给学生。"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveApplicationMapper leaveMapper;
    private final LeaveLogMapper logMapper;
    private final OnlineUserService onlineUserService;

    @Override
    @Transactional
    public LeaveApplication apply(Long applicantId, Long approverId, int type,
                                   String reason, LocalDateTime startTime, LocalDateTime endTime) {
        LeaveApplication app = new LeaveApplication();
        app.setApplicantId(applicantId);
        app.setApproverId(approverId);
        app.setType(type);
        app.setReason(reason);
        app.setStartTime(startTime);
        app.setEndTime(endTime);
        app.setStatus(LeaveApplication.STATUS_PENDING);
        app.setCreateTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        leaveMapper.insert(app);

        // 写入操作日志
        writeLog(app.getId(), null, LeaveApplication.STATUS_PENDING, applicantId, "提交申请");

        // 对齐论文 5.4：通过 WebSocket 向教师推送请假卡片
        Map<String, Object> card = new HashMap<>();
        card.put("event", "leave_card");
        card.put("leaveId", app.getId());
        card.put("applicantId", applicantId);
        card.put("type", type);
        card.put("reason", reason);
        card.put("startTime", startTime.toString());
        card.put("endTime", endTime.toString());
        card.put("status", LeaveApplication.STATUS_PENDING);
        onlineUserService.push(approverId, "/queue/messages", card);

        return app;
    }

    @Override
    @Transactional
    public void approve(Long approverId, Long leaveId, String note) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApproverId().equals(approverId), ResultCode.LEAVE_NOT_APPROVER);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_APPROVED, approverId, note);

        // 即时反馈给学生
        pushResult(app, "approved", note);
    }

    @Override
    @Transactional
    public void reject(Long approverId, Long leaveId, String note) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApproverId().equals(approverId), ResultCode.LEAVE_NOT_APPROVER);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_REJECTED, approverId, note);

        pushResult(app, "rejected", note);
    }

    @Override
    @Transactional
    public void revoke(Long applicantId, Long leaveId) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApplicantId().equals(applicantId), ResultCode.LEAVE_STATE_INVALID);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_REVOKED, applicantId, "学生主动撤回");
    }

    @Override
    public List<LeaveApplication> listPending(Long approverId) {
        return leaveMapper.listPendingByApprover(approverId);
    }

    @Override
    public List<LeaveApplication> listMine(Long applicantId) {
        return leaveMapper.listByApplicant(applicantId);
    }

    // ==================== 内部方法 ====================

    private LeaveApplication getAndCheck(Long leaveId) {
        LeaveApplication app = leaveMapper.selectById(leaveId);
        BusinessException.check(app != null, ResultCode.NOT_FOUND);
        return app;
    }

    /** 状态机流转核心 */
    private void transition(LeaveApplication app, int toStatus, Long operatorId, String note) {
        int fromStatus = app.getStatus();
        app.setStatus(toStatus);
        app.setApproveNote(note);
        app.setApproveTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        leaveMapper.updateById(app);

        writeLog(app.getId(), fromStatus, toStatus, operatorId, note);
    }

    /** 对齐论文 5.4："每次状态变更均记录操作时间和操作人，形成完整的操作日志" */
    private void writeLog(Long leaveId, Integer from, int to, Long operatorId, String remark) {
        LeaveLog l = new LeaveLog();
        l.setLeaveId(leaveId);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setOperatorId(operatorId);
        l.setRemark(remark);
        l.setCreateTime(LocalDateTime.now());
        logMapper.insert(l);
    }

    /** "审批结果即时反馈给学生" */
    private void pushResult(LeaveApplication app, String action, String note) {
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "leave_result");
        evt.put("leaveId", app.getId());
        evt.put("action", action);
        evt.put("note", note);
        onlineUserService.push(app.getApplicantId(), "/queue/messages", evt);
    }
}