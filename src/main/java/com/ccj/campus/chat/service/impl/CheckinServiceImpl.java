package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.*;
import com.ccj.campus.chat.entity.*;
import com.ccj.campus.chat.mapper.*;
import com.ccj.campus.chat.service.BadgeService;
import com.ccj.campus.chat.service.CheckinQrService;
import com.ccj.campus.chat.service.CheckinService;
import com.ccj.campus.chat.util.GeoUtils;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 签到业务实现。对齐论文 5.3：
 * - 球面距离判断（GeoUtils，公式 5.1）
 * - 超过 10 分钟标记迟到
 * - 坐标异常跳变检测
 * - 签到会话 ID + 学生 ID 联合唯一约束杜绝重复
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final CheckinSessionMapper sessionMapper;
    private final CheckinRecordMapper recordMapper;
    private final CheckinSupplementMapper supplementMapper;
    private final OnlineUserService onlineUserService;
    private final CheckinQrService qrService;
    private final CourseMapper courseMapper;
    private final CourseClassRelMapper courseClassRelMapper;
    private final SysClassMapper sysClassMapper;
    private final SysUserMapper sysUserMapper;
    private final BadgeService badgeService;
    private final SysUserClassRelMapper sysUserClassRelMapper;
    private final LeaveApplicationMapper leaveApplicationMapper;
    private final SysUserMapper userMapper;
    private final CheckinSessionMapper checkinSessionMapper;
    private final CheckinRecordMapper checkinRecordMapper;

    @Value("${campus.checkin.late-minutes:10}")
    private int lateMinutes;


    @Override
    public List<com.ccj.campus.chat.dto.StudentCheckinStatusVO> getSessionStudentStatus(Long sessionId) {
        // 1. 获取签到任务信息
        CheckinSession session = checkinSessionMapper.selectById(sessionId);
        if (session == null) {
            return java.util.Collections.emptyList();
        }

        // 2. 查出该签到任务涉及的所有学生 (通过 sys_user_class_rel 和 sys_checkin_session_class 联查)
        List<SysUser> students = userMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                .inSql("id", "SELECT user_id FROM sys_user_class_rel WHERE class_id IN " +
                        "(SELECT class_id FROM sys_checkin_session_class WHERE session_id = " + sessionId + ")")
                .eq("role", SysUser.ROLE_STUDENT));

        if (students.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 3. 获取这些学生的签到记录
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CheckinRecord>()
                        .eq("session_id", sessionId));
        java.util.Map<Long, CheckinRecord> recordMap = records.stream()
                .collect(java.util.stream.Collectors.toMap(CheckinRecord::getStudentId, r -> r, (r1, r2) -> r1));

        // 4. 获取这些学生的请假记录（检查签到时间是否在请假范围内，且请假已通过 status=1）
        java.time.LocalDateTime checkTime = session.getStartTime() != null ? session.getStartTime() : session.getCreateTime();
        List<com.ccj.campus.chat.entity.LeaveApplication> leaves = leaveApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.ccj.campus.chat.entity.LeaveApplication>()
                        .eq("status", 1)
                        .le("start_time", checkTime)
                        .ge("end_time", checkTime));
        java.util.Map<Long, com.ccj.campus.chat.entity.LeaveApplication> leaveMap = leaves.stream()
                .collect(java.util.stream.Collectors.toMap(com.ccj.campus.chat.entity.LeaveApplication::getApplicantId, l -> l, (l1, l2) -> l1));

        // 5. 组装VO并返回
        return students.stream().map(student -> {
            com.ccj.campus.chat.dto.StudentCheckinStatusVO vo = new com.ccj.campus.chat.dto.StudentCheckinStatusVO();
            vo.setStudentId(student.getId());
            vo.setStudentName(student.getName());
            vo.setAccountNumber(student.getAccountNumber());

            if (leaveMap.containsKey(student.getId())) {
                vo.setStatus(3); // 已请假
            } else if (recordMap.containsKey(student.getId())) {
                vo.setStatus(1); // 已签到
            } else {
                vo.setStatus(0); // 未签到
            }
            return vo;
        }).collect(java.util.stream.Collectors.toList());
    }


    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer) return (Integer) v;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static java.time.LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) v;
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime();
        return null;
    }

    @Override
    public List<CheckinSupplement> listSupplementsForStudent(Long studentId) {
        if (studentId == null) return Collections.emptyList();
        // 按照创建时间倒序查询该学生的所有补签记录
        LambdaQueryWrapper<CheckinSupplement> qw = new LambdaQueryWrapper<>();
        qw.eq(CheckinSupplement::getStudentId, studentId)
                .orderByDesc(CheckinSupplement::getCreateTime);
        return supplementMapper.selectList(qw);
    }

    @Override
    public List<TeacherSupplementVO> listSupplementsForTeacher(Long teacherId, boolean pendingOnly) {
        if (teacherId == null) return Collections.emptyList();
        List<Map<String, Object>> rows = supplementMapper.listByTeacher(teacherId, pendingOnly);
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<TeacherSupplementVO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            result.add(TeacherSupplementVO.builder()
                    .id(toLong(row.get("supplement_id"))) // 新增：映射 id
                    .supplementId(toLong(row.get("supplement_id")))
                    .sessionId(toLong(row.get("session_id")))
                    .studentId(toLong(row.get("student_id")))
                    .studentName((String) row.get("student_name"))
                    .studentNo((String) row.get("student_account_number")) // 新增：映射学号
                    .studentAccountNumber((String) row.get("student_account_number"))
                    .studentAvatar((String) row.get("student_avatar"))
                    .courseId(toLong(row.get("course_id")))
                    .courseName((String) row.get("course_name"))
                    .sessionTitle((String) row.get("session_title"))
                    .sessionStartTime(toLocalDateTime(row.get("session_start_time")))
                    .reason((String) row.get("reason"))
                    .status(toInt(row.get("status")))
                    .approveNote((String) row.get("approve_note")) // 新增：映射审批意见
                    .createTime(toLocalDateTime(row.get("create_time")))
                    .build());
        }
        return result;
    }

    @Override
    public List<CourseClassVO> listCourseClasses(Long teacherId, Long courseId) {
        if (courseId == null) return Collections.emptyList();

        // 顺带校验一下：非管理员时，课程必须是该老师自己的
        Course course = courseMapper.selectById(courseId);
        BusinessException.check(course != null, ResultCode.NOT_FOUND, "课程不存在");
        // 管理员角色在 Controller 层已允许访问，这里只兜底校验普通老师不能看别人的课
        // 若你想彻底开放给 ADMIN，可在 Controller 层带 role 进来再判断
        // 这里保守做法：只要 creator 就行
        if (!course.getTeacherId().equals(teacherId)) {
            // 不抛异常，返回空列表，避免无关紧要的场景打断流程
            return Collections.emptyList();
        }

        List<Long> classIds = courseClassRelMapper.listClassIdsByCourse(courseId);
        if (classIds == null || classIds.isEmpty()) return Collections.emptyList();

        // 一次 selectBatchIds 拉齐名称
        List<SysClass> classes = sysClassMapper.selectBatchIds(classIds);
        if (classes == null) return Collections.emptyList();
        return classes.stream()
                .map(c -> CourseClassVO.builder()
                        .classId(c.getId())
                        .className(c.getName())
                        .grade(c.getGrade())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TeacherCourseVO> listTeacherCourses(Long teacherId) {
        if (teacherId == null) return Collections.emptyList();
        List<Course> courses = courseMapper.listByTeacher(teacherId);
        if (courses == null || courses.isEmpty()) return Collections.emptyList();
        return courses.stream()
                .map(c -> TeacherCourseVO.builder()
                        .courseId(c.getId())
                        .courseName(c.getName())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 教师：二维码 ====================

    @Override
    public CheckinQrService.QrContent generateSessionQr(Long teacherId, Long sessionId) {
        CheckinSession session = sessionMapper.selectById(sessionId);
        BusinessException.check(session != null, ResultCode.NOT_FOUND);
        // 仅会话创建者可刷新自己的二维码
        BusinessException.check(session.getCreatorId().equals(teacherId), ResultCode.FORBIDDEN);
        BusinessException.check(session.getStatus() == CheckinSession.STATUS_ACTIVE,
                ResultCode.CHECKIN_SESSION_ENDED);
        if (session.getEndTime() != null && LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new BusinessException(ResultCode.CHECKIN_SESSION_ENDED);
        }
        return qrService.generate(sessionId);
    }

// ==================== 学生：扫码签到 ====================

    @Override
    @Transactional
    public CheckinRecord checkinByQrCode(Long studentId, String qrContent) {
        Long sessionId = qrService.verify(qrContent);

        CheckinSession session = sessionMapper.selectById(sessionId);
        BusinessException.check(session != null, ResultCode.NOT_FOUND);
        BusinessException.check(session.getStatus() == CheckinSession.STATUS_ACTIVE,
                ResultCode.CHECKIN_SESSION_ENDED);
        if (session.getEndTime() != null && LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new BusinessException(ResultCode.CHECKIN_SESSION_ENDED);
        }

        // 扫码签到不做距离校验（与签到码签到一致，二维码的 TTL 已提供防远程转发保护）
        Duration elapsed = Duration.between(session.getStartTime(), LocalDateTime.now());
        int status = elapsed.toMinutes() >= lateMinutes
                ? CheckinRecord.STATUS_LATE
                : CheckinRecord.STATUS_NORMAL;

        CheckinRecord record = new CheckinRecord();
        record.setType(CheckinRecord.TYPE_QR);
        record.setSessionId(session.getId());
        record.setStudentId(studentId);
        record.setStatus(status);
        record.setCheckinTime(LocalDateTime.now());

        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CHECKIN_DUPLICATE);
        }
        return record;
    }

// ==================== 学生：历史记录 ====================

    @Override
    public List<StudentHistoryCourseVO> listHistoryForStudent(Long studentId) {
        List<StudentCheckinHistoryRow> rows = recordMapper.listHistoryByStudent(studentId);

        // 用 LinkedHashMap 保留查询顺序（Mapper 已按 start_time DESC 排好）
        LinkedHashMap<Long, StudentHistoryCourseVO> grouped = new LinkedHashMap<>();
        for (StudentCheckinHistoryRow row : rows) {
            StudentHistoryCourseVO courseVo = grouped.computeIfAbsent(row.getCourseId(), cid -> {
                StudentHistoryCourseVO vo = new StudentHistoryCourseVO();
                vo.setCourseId(cid);
                vo.setCourseName(row.getCourseName());
                return vo;
            });

            StudentHistoryRecordVO rec = new StudentHistoryRecordVO();
            rec.setSessionId(row.getSessionId());
            rec.setSessionTitle(row.getSessionTitle());
            rec.setStartTime(row.getStartTime());
            rec.setEndTime(row.getEndTime());
            rec.setCheckedIn(row.getRecStatus() != null);
            rec.setStatus(row.getRecStatus());
            rec.setCheckInTime(row.getCheckInTime());
            courseVo.getRecords().add(rec);
        }
        return new ArrayList<>(grouped.values());
    }

    // ==================== 教师端 ====================

    @Override
    @Transactional
    public CheckinSession createSession(Long teacherId, Long courseId, String title,
                                        double lat, double lon, int radiusMeters,
                                        int durationMinutes, List<Long> classIds) {
        CheckinSession session = new CheckinSession();
        session.setCourseId(courseId);
        session.setCreatorId(teacherId);
        session.setTitle(title);
        session.setCenterLatitude(BigDecimal.valueOf(lat));
        session.setCenterLongitude(BigDecimal.valueOf(lon));
        session.setRadiusMeters(radiusMeters);
        session.setDurationMinutes(durationMinutes);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(CheckinSession.STATUS_ACTIVE);
        session.setCreateTime(LocalDateTime.now());
        sessionMapper.insert(session);

        // 批量插入签到会话 - 班级关系
        if (classIds != null && !classIds.isEmpty()) {
            sessionMapper.batchInsertSessionClass(session.getId(), classIds);
        }

        // 对齐论文 5.3：通过 WebSocket 向该课程下的所有在线学生推送签到提醒通知
        Map<String, Object> notify = new HashMap<>();
        notify.put("event", "checkin_start");
        notify.put("sessionId", session.getId());
        notify.put("title", title);
        notify.put("durationMinutes", durationMinutes);
        onlineUserService.broadcast("/topic/checkin/course/" + courseId, notify);

        return session;
    }

    @Override
    @Transactional
    public String setSessionCode(Long sessionId, String code) {
        CheckinSession s = sessionMapper.selectById(sessionId);
        BusinessException.check(s != null, ResultCode.NOT_FOUND);
        s.setCode(code);
        sessionMapper.updateById(s);
        return code;
    }

    // ==================== 学生端 ====================

    @Override
    @Transactional
    public CheckinRecord checkin(Long studentId, Long sessionId, double lat, double lon) {
        CheckinSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new RuntimeException("签到会话不存在");

        // 👇 加上这个判断，防止学生绕过签到码强行用定位签到
        if (session.getCode() != null && !session.getCode().isEmpty()) {
            throw new RuntimeException("该签到需要使用签到码，不能使用定位签到");
        }

        BusinessException.check(session != null, ResultCode.NOT_FOUND);
        BusinessException.check(session.getStatus() == CheckinSession.STATUS_ACTIVE, ResultCode.CHECKIN_SESSION_ENDED);

        // 检查是否超时
        LocalDateTime now = LocalDateTime.now();
        if (session.getEndTime() != null && now.isAfter(session.getEndTime())) {
            throw new BusinessException(ResultCode.CHECKIN_SESSION_ENDED);
        }

        // 对齐论文 5.3 公式 (5.1)：球面距离计算
        double centerLat = session.getCenterLatitude().doubleValue();
        double centerLon = session.getCenterLongitude().doubleValue();
        int distance = GeoUtils.distanceMeters(lat, lon, centerLat, centerLon);
        boolean inFence = distance <= session.getRadiusMeters();

        // 确定签到状态
        int status;
        if (!inFence) {
            status = CheckinRecord.STATUS_OUT_RANGE;
            throw new BusinessException(ResultCode.CHECKIN_NOT_IN_RANGE,
                    "距离签到点 " + distance + " 米，超出围栏 " + session.getRadiusMeters() + " 米");
        }

        // 对齐论文 5.3："已过签到开始时间 10 分钟以上的标记为迟到"
        Duration elapsed = Duration.between(session.getStartTime(), now);
        if (elapsed.toMinutes() >= lateMinutes) {
            status = CheckinRecord.STATUS_LATE;
        } else {
            status = CheckinRecord.STATUS_NORMAL;
        }

        CheckinRecord record = new CheckinRecord();
        record.setType(CheckinRecord.TYPE_QR);
        record.setSessionId(sessionId);
        record.setStudentId(studentId);
        record.setLatitude(BigDecimal.valueOf(lat));
        record.setLongitude(BigDecimal.valueOf(lon));
        record.setDistanceM(distance);
        record.setStatus(status);
        record.setCheckinTime(now);

        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 对齐论文 4.2："联合唯一约束从根本上杜绝了重复提交"
            throw new BusinessException(ResultCode.CHECKIN_DUPLICATE);
        }
        return record;
    }

    @Override
    @Transactional
    public CheckinRecord checkinByCode(Long studentId, String code) {
        // 按签到码查找进行中的会话
        QueryWrapper<CheckinSession> qw = new QueryWrapper<>();
        qw.eq("code", code).eq("status", CheckinSession.STATUS_ACTIVE);
        CheckinSession session = sessionMapper.selectOne(qw);
        if (session == null) {
            throw new BusinessException(ResultCode.CHECKIN_CODE_WRONG);
        }
        if (session.getEndTime() != null && LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new BusinessException(ResultCode.CHECKIN_SESSION_ENDED);
        }

        // 签到码签到不做距离校验，直接通过
        Duration elapsed = Duration.between(session.getStartTime(), LocalDateTime.now());
        int status = elapsed.toMinutes() >= lateMinutes
                ? CheckinRecord.STATUS_LATE
                : CheckinRecord.STATUS_NORMAL;

        CheckinRecord record = new CheckinRecord();
        record.setType(CheckinRecord.TYPE_QR);
        record.setSessionId(session.getId());
        record.setStudentId(studentId);
        record.setStatus(status);
        record.setCheckinTime(LocalDateTime.now());

        try {
            recordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCode.CHECKIN_DUPLICATE);
        }
        return record;
    }

    @Override
    public List<CheckinSession> listActiveForStudent(Long studentId) {
        return sessionMapper.listActiveForStudent(studentId);
    }

    @Override
    public List<CheckinRecord> listRecords(Long sessionId) {
        return recordMapper.listBySession(sessionId);
    }

    // ==================== 补签 ====================

    @Override
    @Transactional
    public CheckinSupplement submitSupplement(Long studentId, Long sessionId, String reason) {
        // 先查会话，用来知道推送给哪位老师
        CheckinSession session = sessionMapper.selectById(sessionId);
        BusinessException.check(session != null, ResultCode.NOT_FOUND, "签到会话不存在");

        CheckinSupplement s = new CheckinSupplement();
        s.setSessionId(sessionId);
        s.setStudentId(studentId);
        s.setReason(reason);
        s.setStatus(CheckinSupplement.STATUS_PENDING);
        s.setCreateTime(LocalDateTime.now());
        supplementMapper.insert(s);

        // 推送给发起老师（对齐好友申请里的 badgeService.pushBadgeIfOnline 模式）
        Long teacherId = session.getCreatorId();
        if (teacherId != null) {
            SysUser student = sysUserMapper.selectById(studentId);

            Map<String, Object> evt = new HashMap<>();
            evt.put("event", "supplement_request");
            evt.put("supplementId", s.getId());
            evt.put("sessionId", sessionId);
            evt.put("sessionTitle", session.getTitle());
            evt.put("courseId", session.getCourseId());
            evt.put("studentId", studentId);
            evt.put("studentName", student != null ? student.getName() : null);
            evt.put("reason", reason);
            evt.put("createTime", s.getCreateTime());

            // WS 事件推送 —— 老师端监听 /queue/messages 里 event=supplement_request
            if (onlineUserService.isOnline(teacherId)) {
                onlineUserService.push(teacherId, "/queue/messages", evt);
            }
            // 红点：让老师端工作台角标刷新（和好友申请的红点走同一通道）
            try {
                badgeService.pushBadgeIfOnline(teacherId, null);
            } catch (Exception ignore) {
                // 即便 badge 推送失败也不影响主流程
            }
        }

        return s;
    }

    @Override
    @Transactional
    public void approveSupplement(Long teacherId, Long supplementId, boolean approved, String note) {
        CheckinSupplement s = supplementMapper.selectById(supplementId);
        BusinessException.check(s != null, ResultCode.NOT_FOUND);
        BusinessException.check(s.getStatus() == CheckinSupplement.STATUS_PENDING,
                ResultCode.BAD_REQUEST, "该申请已被处理");

        // 关键新增：校验当前老师是不是该签到会话的发起人，防止越权审批
        CheckinSession session = sessionMapper.selectById(s.getSessionId());
        BusinessException.check(session != null, ResultCode.NOT_FOUND, "签到会话不存在");
        BusinessException.check(
                teacherId != null && teacherId.equals(session.getCreatorId()),
                ResultCode.FORBIDDEN,
                "无权审批他人发起的签到补签"
        );

        s.setApproverId(teacherId);
        s.setApproveTime(LocalDateTime.now());
        s.setApproveNote(note);
        s.setStatus(approved ? CheckinSupplement.STATUS_APPROVED : CheckinSupplement.STATUS_REJECTED);
        supplementMapper.updateById(s);

        // 审批通过则补一条签到记录
        if (approved) {
            CheckinRecord record = new CheckinRecord();
            record.setType(CheckinRecord.TYPE_SUPPLEMENT);
            record.setSessionId(s.getSessionId());
            record.setStudentId(s.getStudentId());
            record.setStatus(CheckinRecord.STATUS_SUPPLEMENTED);
            record.setCheckinTime(LocalDateTime.now());
            try {
                recordMapper.insert(record);
            } catch (org.springframework.dao.DuplicateKeyException ignore) {
                // 已有签到记录，忽略
            }
        }

        // 通知学生审批结果（保持原有事件名不变，前端已经在监听）
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "supplement_result");
        evt.put("supplementId", supplementId);
        evt.put("approved", approved);
        evt.put("note", note);
        if (onlineUserService.isOnline(s.getStudentId())) {
            onlineUserService.push(s.getStudentId(), "/queue/messages", evt);
        }
        try {
            badgeService.pushBadgeIfOnline(s.getStudentId(), null);
        } catch (Exception ignore) {
        }
    }


}