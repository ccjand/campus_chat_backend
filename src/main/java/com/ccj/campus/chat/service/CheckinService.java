package com.ccj.campus.chat.service;

import com.ccj.campus.chat.entity.CheckinRecord;
import com.ccj.campus.chat.entity.CheckinSession;
import com.ccj.campus.chat.entity.CheckinSupplement;

import java.util.List;

/**
 * 签到业务接口。对齐论文 5.3：
 *   - 教师发起签到（含围栏参数、签到码）
 *   - 学生提交签到（坐标判距 + 迟到判定）
 *   - 补签申请与审批
 */
public interface CheckinService {

    /** 教师：发起签到会话 */
    CheckinSession createSession(Long teacherId, Long courseId, String title,
                                  double lat, double lon, int radiusMeters,
                                  int durationMinutes, List<Long> classIds);

    /** 教师：为签到会话设置签到码 */
    String setSessionCode(Long sessionId, String code);

    /** 学生：GPS 签到 */
    CheckinRecord checkin(Long studentId, Long sessionId, double lat, double lon);

    /** 学生：签到码签到 */
    CheckinRecord checkinByCode(Long studentId, String code);

    /** 学生：获取当前活跃签到 */
    List<CheckinSession> listActiveForStudent(Long studentId);

    /** 教师：查看某次签到的全部记录 */
    List<CheckinRecord> listRecords(Long sessionId);

    /** 学生：提交补签申请 */
    CheckinSupplement submitSupplement(Long studentId, Long sessionId, String reason);

    /** 教师：审批补签 */
    void approveSupplement(Long teacherId, Long supplementId, boolean approved, String note);
}