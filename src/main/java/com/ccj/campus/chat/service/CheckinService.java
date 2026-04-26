package com.ccj.campus.chat.service;

import com.ccj.campus.chat.dto.*;
import com.ccj.campus.chat.entity.CheckinRecord;
import com.ccj.campus.chat.entity.CheckinSession;
import com.ccj.campus.chat.entity.CheckinSupplement;

import java.util.List;

/**
 * 签到业务接口。对齐论文 5.3：
 * - 教师发起签到（含围栏参数、签到码、二维码）
 * - 学生提交签到（坐标判距 + 迟到判定 / 签到码 / 扫码）
 * - 补签申请与审批（含推送通知）
 * - 学生查询自己的历史签到
 */
public interface CheckinService {

    /**
     * 教师：获取某次签到的全班名单状态（包含已签到、已请假、未签到）
     */
    List<StudentCheckinStatusVO> getSessionStudentStatus(Long sessionId);

    // ==================== 教师下拉数据 ====================

    /**
     * 教师端：我负责的课程列表（用于发起签到时选择课程）
     */
    List<TeacherCourseVO> listTeacherCourses(Long teacherId);

    /**
     * 教师端：某课程关联的班级列表（用于发起签到时选择班级范围）
     */
    List<CourseClassVO> listCourseClasses(Long teacherId, Long courseId);

    // ==================== 教师端 ====================

    /**
     * 教师：发起签到会话
     */
    CheckinSession createSession(Long teacherId, Long courseId, String title,
                                 Double lat, Double lon, Integer radiusMeters,
                                 int durationMinutes, List<Long> classIds);

    /**
     * 教师：为签到会话设置签到码
     */
    String setSessionCode(Long sessionId, String code);

    /**
     * 教师：为签到会话生成/刷新二维码（60s TTL）
     */
    CheckinQrService.QrContent generateSessionQr(Long teacherId, Long sessionId);

    /**
     * 教师：查看某次签到的全部记录
     */
    List<CheckinRecord> listRecords(Long sessionId);

    /**
     * 教师：查看自己发起的签到被提交的补签申请列表；pendingOnly=true 只返回待审批
     */
    List<TeacherSupplementVO> listSupplementsForTeacher(Long teacherId, boolean pendingOnly);

    /**
     * 教师：审批补签
     */
    void approveSupplement(Long teacherId, Long supplementId, boolean approved, String note);

    // ==================== 学生端 ====================

    /**
     * 学生：获取我的补签记录
     */
    List<CheckinSupplement> listSupplementsForStudent(Long studentId);

    /**
     * 学生：GPS 签到
     */
    CheckinRecord checkin(Long studentId, Long sessionId, double lat, double lon);

    /**
     * 学生：签到码签到
     */
    CheckinRecord checkinByCode(Long studentId, String code);

    /**
     * 学生：扫码签到（二维码内容由 CheckinQrService 签发）
     */
    CheckinRecord checkinByQrCode(Long studentId, String qrContent);

    /**
     * 学生：获取当前活跃签到
     */
    List<CheckinSession> listActiveForStudent(Long studentId);

    /**
     * 学生：查询自己的历史签到（按课程聚合）
     */
    List<StudentHistoryCourseVO> listHistoryForStudent(Long studentId);

    /**
     * 学生：提交补签申请（会同步向发起老师推送通知）
     */
    CheckinSupplement submitSupplement(Long studentId, Long sessionId, String reason);
}