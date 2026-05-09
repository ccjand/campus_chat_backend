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
import java.util.Map;

/**
 * 请假接口。对齐论文 5.4：
 * "审批人可直接在聊天界面内嵌的请假卡片上完成审批操作，审批结果即时反馈给学生。"
 * <p>
 * 审批权限：
 * 辅导员(ROLE_COUNSELOR) —— 审批自己管理班级的学生请假
 * 院长(ROLE_STAFF)       —— 审批自己管理的教师/辅导员请假
 */
@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    // 学生、教师、辅导员 都可以发起请假（辅导员向院长请假）
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_COUNSELOR')")
    @PostMapping("/apply")
    public R<LeaveApplication> apply(@RequestBody @Valid LeaveApplyReq req) {
        return R.ok(leaveService.apply(
                LoginUser.currentUid(),
                req.getApproverId(),
                req.getType(),
                req.getReason(),
                req.getStartTime(),
                req.getEndTime(),
                req.getAttachments()
        ));
    }

    /**
     * 审批人：通过（只有辅导员和院长）
     */
    @PreAuthorize("hasAnyAuthority('ROLE_COUNSELOR','ROLE_STAFF')")
    @PostMapping("/approve")
    public R<Void> approve(@RequestBody @Valid ApproveReq req) {
        leaveService.approve(LoginUser.currentUid(), req.getLeaveId(), req.getNote());
        return R.ok();
    }

    /**
     * 审批人：驳回（只有辅导员和院长）
     */
    @PreAuthorize("hasAnyAuthority('ROLE_COUNSELOR','ROLE_STAFF')")
    @PostMapping("/reject")
    public R<Void> reject(@RequestBody @Valid ApproveReq req) {
        leaveService.reject(LoginUser.currentUid(), req.getLeaveId(), req.getNote());
        return R.ok();
    }

    // 学生、教师、辅导员 都可以撤销自己的请假
    @PreAuthorize("hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_COUNSELOR')")
    @PostMapping("/revoke")
    public R<Void> revoke(@RequestParam Long leaveId) {
        leaveService.revoke(LoginUser.currentUid(), leaveId);
        return R.ok();
    }

    /**
     * 审批人：待审列表（只有辅导员和院长）
     */
    @PreAuthorize("hasAnyAuthority('ROLE_COUNSELOR','ROLE_STAFF')")
    @GetMapping("/pending")
    public R<List<LeaveApplication>> pending() {
        return R.ok(leaveService.listPending(LoginUser.currentUid()));
    }

    /**
     * 申请人：自己的请假历史
     */
    @GetMapping("/mine")
    public R<List<LeaveApplication>> mine() {
        return R.ok(leaveService.listMine(LoginUser.currentUid()));
    }

    /**
     * 申请人：标记审批结果已读
     */
    @PostMapping("/markResultRead")
    public R<Void> markResultRead() {
        Long uid = LoginUser.currentUid();
        leaveService.markResultRead(uid);
        return R.ok();
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

        private List<Map<String, Object>> attachments;
    }

    @Data
    static class ApproveReq {
        @NotNull
        private Long leaveId;
        private String note;
    }
}