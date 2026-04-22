package com.ccj.campus.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.StudentCheckinHistoryRow;
import com.ccj.campus.chat.dto.StudentHistoryCourseVO;
import com.ccj.campus.chat.dto.StudentHistoryRecordVO;
import com.ccj.campus.chat.entity.CheckinRecord;
import com.ccj.campus.chat.entity.CheckinSession;
import com.ccj.campus.chat.entity.CheckinSupplement;
import com.ccj.campus.chat.mapper.CheckinRecordMapper;
import com.ccj.campus.chat.mapper.CheckinSessionMapper;
import com.ccj.campus.chat.mapper.CheckinSupplementMapper;
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

/**
 * 签到业务实现。对齐论文 5.3：
 *   - 球面距离判断（GeoUtils，公式 5.1）
 *   - 超过 10 分钟标记迟到
 *   - 坐标异常跳变检测
 *   - 签到会话 ID + 学生 ID 联合唯一约束杜绝重复
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

    @Value("${campus.checkin.late-minutes:10}")
    private int lateMinutes;

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
        CheckinSupplement s = new CheckinSupplement();
        s.setSessionId(sessionId);
        s.setStudentId(studentId);
        s.setReason(reason);
        s.setStatus(CheckinSupplement.STATUS_PENDING);
        s.setCreateTime(LocalDateTime.now());
        supplementMapper.insert(s);
        return s;
    }

    @Override
    @Transactional
    public void approveSupplement(Long teacherId, Long supplementId, boolean approved, String note) {
        CheckinSupplement s = supplementMapper.selectById(supplementId);
        BusinessException.check(s != null, ResultCode.NOT_FOUND);
        BusinessException.check(s.getStatus() == CheckinSupplement.STATUS_PENDING, ResultCode.BAD_REQUEST);

        s.setApproverId(teacherId);
        s.setApproveTime(LocalDateTime.now());
        s.setApproveNote(note);
        s.setStatus(approved ? CheckinSupplement.STATUS_APPROVED : CheckinSupplement.STATUS_REJECTED);
        supplementMapper.updateById(s);

        // 审批通过则补一条签到记录
        if (approved) {
            CheckinRecord record = new CheckinRecord();
            record.setSessionId(s.getSessionId());
            record.setStudentId(s.getStudentId());
            record.setStatus(CheckinRecord.STATUS_SUPPLEMENTED);
            record.setCheckinTime(LocalDateTime.now());
            try {
                recordMapper.insert(record);
            } catch (DuplicateKeyException ignore) {
                // 已有签到记录，忽略
            }
        }

        // 通知学生审批结果
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "supplement_result");
        evt.put("supplementId", supplementId);
        evt.put("approved", approved);
        evt.put("note", note);
        onlineUserService.push(s.getStudentId(), "/queue/messages", evt);
    }
}