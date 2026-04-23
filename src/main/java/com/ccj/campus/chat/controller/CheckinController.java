package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.dto.*;
import com.ccj.campus.chat.entity.CheckinRecord;
import com.ccj.campus.chat.entity.CheckinSession;
import com.ccj.campus.chat.entity.CheckinSupplement;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.CheckinQrService;
import com.ccj.campus.chat.service.CheckinService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 签到接口。对齐论文 4.3：
 * - 教师端：/checkin/teacher/**  ROLE_TEACHER / ROLE_ADMIN
 * - 学生端：/checkin/student/**  ROLE_STUDENT
 */
@RestController
@RequestMapping("/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;
    private final CheckinQrService qrService;

    /**
     * 教师端：获取某次签到的全班学生名单及其状态（已签到、已请假、未签到）
     */
    @GetMapping("/teacher/session/{sessionId}/students")
    public R<List<com.ccj.campus.chat.dto.StudentCheckinStatusVO>> getSessionStudents(@PathVariable Long sessionId) {
        return R.ok(checkinService.getSessionStudentStatus(sessionId));
    }

    // ==================== 教师端 ====================

    /**
     * 教师端：我负责的课程下拉列表。
     * 对齐前端 /capi/checkin/teacher/courses。
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/teacher/courses")
    public R<List<TeacherCourseVO>> myCourses() {
        return R.ok(checkinService.listTeacherCourses(LoginUser.currentUid()));
    }

    /**
     * 教师端：课程对应的班级列表（供发起签到时选择班级范围）。
     * 对齐前端 /capi/checkin/teacher/course/{courseId}/classes。
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/teacher/course/{courseId}/classes")
    public R<List<CourseClassVO>> courseClasses(@PathVariable Long courseId) {
        return R.ok(checkinService.listCourseClasses(LoginUser.currentUid(), courseId));
    }

    /**
     * 教师发起签到
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/session")
    public R<CheckinSession> createSession(@RequestBody @Valid CreateSessionReq req) {
        Long uid = LoginUser.currentUid();
        CheckinSession session = checkinService.createSession(uid, req.getCourseId(), req.getTitle(),
                req.getCenterLatitude(), req.getCenterLongitude(),
                req.getRadiusMeters(), req.getDurationMinutes(), req.getClassIds());
        return R.ok(session);
    }

    /**
     * 教师设置签到码
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/session/{sessionId}/code")
    public R<String> setCode(@PathVariable Long sessionId, @RequestBody CodeReq req) {
        return R.ok(checkinService.setSessionCode(sessionId, req.getCode()));
    }

    /**
     * 教师生成/刷新签到二维码。
     * 返回: { sessionId, content, imageBase64, expireAt }
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/session/{sessionId}/qrcode")
    public R<Map<String, Object>> getSessionQr(@PathVariable Long sessionId) {
        Long uid = LoginUser.currentUid();
        CheckinQrService.QrContent qr = checkinService.generateSessionQr(uid, sessionId);
        String image = qrService.generateImageBase64(qr.getContent());

        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionId", sessionId);
        resp.put("content", qr.getContent());
        resp.put("imageBase64", image);
        resp.put("expireAt", qr.getExpireAt());
        return R.ok(resp);
    }

    /**
     * 教师查看签到记录
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/teacher/session/{sessionId}/records")
    public R<List<CheckinRecord>> records(@PathVariable Long sessionId) {
        return R.ok(checkinService.listRecords(sessionId));
    }

    /**
     * 教师端：待我审批的补签申请列表。
     * 对齐好友申请 /friend/request/received。
     * 默认只返回 status=0 (pending)；传 all=true 则返回全部（含已批/已拒）。
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @GetMapping("/teacher/supplements")
    public R<List<TeacherSupplementVO>> mySupplements(
            @RequestParam(defaultValue = "false") boolean all) {
        return R.ok(checkinService.listSupplementsForTeacher(LoginUser.currentUid(), !all));
    }

    /**
     * 教师审批补签
     */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/teacher/supplement/{id}/approve")
    public R<Void> approveSupplement(@PathVariable Long id,
                                     @RequestBody ApproveReq req) {
        checkinService.approveSupplement(LoginUser.currentUid(), id, req.isApproved(), req.getNote());
        return R.ok();
    }

    // ==================== 学生端 ====================

    /**
     * 学生：获取我的补签记录
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/student/supplements")
    public R<List<CheckinSupplement>> mySupplementRecords() {
        return R.ok(checkinService.listSupplementsForStudent(LoginUser.currentUid()));
    }


    /**
     * 学生：当前可签到会话
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/student/active")
    public R<List<CheckinSession>> active() {
        return R.ok(checkinService.listActiveForStudent(LoginUser.currentUid()));
    }

    /**
     * 学生：GPS 签到
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/checkin")
    public R<CheckinRecord> checkin(@RequestBody @Valid StudentCheckinReq req) {
        return R.ok(checkinService.checkin(LoginUser.currentUid(),
                req.getSessionId(), req.getLatitude(), req.getLongitude()));
    }

    /**
     * 学生：签到码签到
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/checkin/code")
    public R<CheckinRecord> checkinByCode(@RequestBody CodeReq req) {
        return R.ok(checkinService.checkinByCode(
                LoginUser.currentUid(), req.getCode()));
    }

    /**
     * 学生：扫码签到（前端 uni.scanCode 拿到的 content 直接透传上来）
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/checkin/qrcode")
    public R<CheckinRecord> checkinByQrCode(@RequestBody QrCheckinReq req) {
        return R.ok(checkinService.checkinByQrCode(LoginUser.currentUid(), req.getContent()));
    }

    /**
     * 学生：我的签到历史（按课程聚合）
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/student/history")
    public R<List<StudentHistoryCourseVO>> history() {
        return R.ok(checkinService.listHistoryForStudent(LoginUser.currentUid()));
    }

    /**
     * 学生：补签申请
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @PostMapping("/student/supplement")
    public R<CheckinSupplement> supplement(@RequestBody @Valid SupplementReq req) {
        return R.ok(checkinService.submitSupplement(
                LoginUser.currentUid(), req.getSessionId(), req.getReason()));
    }

    // ==================== DTO ====================

    @Data
    static class CreateSessionReq {
        @NotNull
        private Long courseId;
        private String title;
        @NotNull
        private Double centerLatitude;
        @NotNull
        private Double centerLongitude;
        @NotNull
        private Integer radiusMeters;
        @NotNull
        private Integer durationMinutes;
        private List<Long> classIds;
    }

    @Data
    static class StudentCheckinReq {
        @NotNull
        private Long sessionId;
        @NotNull
        private Double latitude;
        @NotNull
        private Double longitude;
    }

    @Data
    static class CodeReq {
        private String code;
    }

    @Data
    static class QrCheckinReq {
        private String content;
    }

    @Data
    static class SupplementReq {
        @NotNull
        private Long sessionId;
        private String reason;
    }

    @Data
    static class ApproveReq {
        private boolean approved;
        private String note;
    }
}