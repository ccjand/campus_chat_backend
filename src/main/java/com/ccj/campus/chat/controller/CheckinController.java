package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.CheckinRecord;
import com.ccj.campus.chat.entity.CheckinSession;
import com.ccj.campus.chat.entity.CheckinSupplement;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.CheckinService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 签到接口。对齐论文 4.3：
 *   - 教师端：/checkin/teacher/**  ROLE_TEACHER / ROLE_ADMIN
 *   - 学生端：/checkin/student/**  ROLE_STUDENT
 */
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    // ==================== 教师端 ====================

    /** 教师发起签到 */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/session")
    public R<CheckinSession> createSession(@RequestBody @Valid CreateSessionReq req) {
        Long uid = LoginUser.currentUid();
        CheckinSession session = checkinService.createSession(uid, req.getCourseId(), req.getTitle(),
                req.getCenterLatitude(), req.getCenterLongitude(),
                req.getRadiusMeters(), req.getDurationMinutes(), req.getClassIds());
        return R.ok(session);
    }

    /** 教师设置签到码 */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/session/{sessionId}/code")
    public R<String> setCode(@PathVariable Long sessionId, @RequestBody CodeReq req) {
        return R.ok(checkinService.setSessionCode(sessionId, req.getCode()));
    }

    /** 教师查看签到记录 */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/teacher/session/{sessionId}/records")
    public R<List<CheckinRecord>> records(@PathVariable Long sessionId) {
        return R.ok(checkinService.listRecords(sessionId));
    }

    /** 教师审批补签 */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/supplement/{id}/approve")
    public R<Void> approveSupplement(@PathVariable Long id,
                                      @RequestBody ApproveReq req) {
        checkinService.approveSupplement(LoginUser.currentUid(), id, req.isApproved(), req.getNote());
        return R.ok();
    }

    // ==================== 学生端 ====================

    /** 学生：当前可签到会话 */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/student/active")
    public R<List<CheckinSession>> active() {
        return R.ok(checkinService.listActiveForStudent(LoginUser.currentUid()));
    }

    /** 学生：GPS 签到 */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/checkin")
    public R<CheckinRecord> checkin(@RequestBody @Valid StudentCheckinReq req) {
        return R.ok(checkinService.checkin(LoginUser.currentUid(),
                req.getSessionId(), req.getLatitude(), req.getLongitude()));
    }

    /** 学生：签到码签到 */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/checkin/code")
    public R<CheckinRecord> checkinByCode(@RequestBody CodeReq req) {
        return R.ok(checkinService.checkinByCode(LoginUser.currentUid(), req.getCode()));
    }

    /** 学生：补签申请 */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/supplement")
    public R<CheckinSupplement> supplement(@RequestBody @Valid SupplementReq req) {
        return R.ok(checkinService.submitSupplement(
                LoginUser.currentUid(), req.getSessionId(), req.getReason()));
    }

    // ==================== DTO ====================

    @Data
    static class CreateSessionReq {
        @NotNull private Long courseId;
        private String title;
        @NotNull private Double centerLatitude;
        @NotNull private Double centerLongitude;
        @NotNull private Integer radiusMeters;
        @NotNull private Integer durationMinutes;
        private List<Long> classIds;
    }

    @Data
    static class StudentCheckinReq {
        @NotNull private Long sessionId;
        @NotNull private Double latitude;
        @NotNull private Double longitude;
    }

    @Data
    static class CodeReq { private String code; }

    @Data
    static class SupplementReq {
        @NotNull private Long sessionId;
        private String reason;
    }

    @Data
    static class ApproveReq {
        private boolean approved;
        private String note;
    }
}