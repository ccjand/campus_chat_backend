package com.ccj.campus.chat.service.impl;

import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.entity.LeaveApplication;
import com.ccj.campus.chat.entity.LeaveLog;
import com.ccj.campus.chat.mapper.LeaveApplicationMapper;
import com.ccj.campus.chat.mapper.LeaveLogMapper;
import com.ccj.campus.chat.service.BadgeService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveApplicationMapper leaveMapper;
    private final LeaveLogMapper logMapper;
    private final OnlineUserService onlineUserService;
    private final BadgeService badgeService;

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

        writeLog(app.getId(), null, LeaveApplication.STATUS_PENDING, applicantId, "提交申请");

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

        // 通知审批人工作台 tab 出现红点
        badgeService.pushBadgeIfOnline(approverId, 2);

        return app;
    }

    @Override
    @Transactional
    public void approve(Long approverId, Long leaveId, String note) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApproverId().equals(approverId), ResultCode.LEAVE_NOT_APPROVER);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_APPROVED, approverId, note);
        pushResult(app, "approved", note);

        // 审批后刷新自己的工作台红点（可能清除）
        badgeService.pushBadgeIfOnline(approverId, 2);
    }

    @Override
    @Transactional
    public void reject(Long approverId, Long leaveId, String note) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApproverId().equals(approverId), ResultCode.LEAVE_NOT_APPROVER);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_REJECTED, approverId, note);
        pushResult(app, "rejected", note);

        // 审批后刷新自己的工作台红点
        badgeService.pushBadgeIfOnline(approverId, 2);
    }

    @Override
    @Transactional
    public void revoke(Long applicantId, Long leaveId) {
        LeaveApplication app = getAndCheck(leaveId);
        BusinessException.check(app.getApplicantId().equals(applicantId), ResultCode.LEAVE_STATE_INVALID);
        BusinessException.check(app.getStatus() == LeaveApplication.STATUS_PENDING, ResultCode.LEAVE_STATE_INVALID);

        transition(app, LeaveApplication.STATUS_REVOKED, applicantId, "学生主动撤回");

        // 撤回后刷新审批人的工作台红点
        badgeService.pushBadgeIfOnline(app.getApproverId(), 2);
    }

    @Override
    public List<LeaveApplication> listPending(Long approverId) {
        return leaveMapper.listPendingByApprover(approverId);
    }

    @Override
    public List<LeaveApplication> listMine(Long applicantId) {
        return leaveMapper.listByApplicant(applicantId);
    }

    private LeaveApplication getAndCheck(Long leaveId) {
        LeaveApplication app = leaveMapper.selectById(leaveId);
        BusinessException.check(app != null, ResultCode.NOT_FOUND);
        return app;
    }

    private void transition(LeaveApplication app, int toStatus, Long operatorId, String note) {
        int fromStatus = app.getStatus();
        app.setStatus(toStatus);
        app.setApproveNote(note);
        app.setApproveTime(LocalDateTime.now());
        app.setUpdateTime(LocalDateTime.now());
        leaveMapper.updateById(app);
        writeLog(app.getId(), fromStatus, toStatus, operatorId, note);
    }

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

    private void pushResult(LeaveApplication app, String action, String note) {
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "leave_result");
        evt.put("leaveId", app.getId());
        evt.put("action", action);
        evt.put("note", note);
        onlineUserService.push(app.getApplicantId(), "/queue/messages", evt);
    }
}