package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.LeaveApplication;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.LeaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 请假接口。对齐论文 5.4：
 * "审批人可直接在聊天界面内嵌的请假卡片上完成审批操作，审批结果即时反馈给学生。"
 */
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // 允许学生和教师都能发起请假
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER')")
    @PostMapping("/apply")
    public R<LeaveApplication> apply(@RequestBody @Valid LeaveApplyReq req) {
        return R.ok(leaveService.apply(LoginUser.currentUid(), req.getApproverId(),
                req.getType(), req.getReason(), req.getStartTime(), req.getEndTime()));
    }

    /**
     * 审批人：通过
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/approve")
    public R<Void> approve(@RequestBody @Valid ApproveReq req) {
        leaveService.approve(LoginUser.currentUid(), req.getLeaveId(), req.getNote());
        return R.ok();
    }

    /**
     * 审批人：驳回
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/reject")
    public R<Void> reject(@RequestBody @Valid ApproveReq req) {
        leaveService.reject(LoginUser.currentUid(), req.getLeaveId(), req.getNote());
        return R.ok();
    }

    // 允许学生和教师都能撤销请假
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER')")
    @PostMapping("/revoke")
    public R<Void> revoke(@RequestParam Long leaveId) {
        leaveService.revoke(LoginUser.currentUid(), leaveId);
        return R.ok();
    }

    /**
     * 审批人：待审列表
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/pending")
    public R<List<LeaveApplication>> pending() {
        return R.ok(leaveService.listPending(LoginUser.currentUid()));
    }

    /**
     * 学生：自己的请假历史
     */
    @GetMapping("/mine")
    public R<List<LeaveApplication>> mine() {
        return R.ok(leaveService.listMine(LoginUser.currentUid()));
    }

    @Data
    static class LeaveApplyReq {
        @NotNull
        private Long approverId;
        @NotNull
        private Integer type;
        @NotBlank
        private String reason;
        @NotNull
        private LocalDateTime startTime;
        @NotNull
        private LocalDateTime endTime;
    }

    @Data
    static class ApproveReq {
        @NotNull
        private Long leaveId;
        private String note;
    }
}