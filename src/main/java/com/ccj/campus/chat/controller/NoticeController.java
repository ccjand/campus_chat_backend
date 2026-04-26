package com.ccj.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.entity.Course;
import com.ccj.campus.chat.entity.Notice;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.mapper.CourseClassRelMapper;
import com.ccj.campus.chat.mapper.CourseMapper;
import com.ccj.campus.chat.mapper.NoticeMapper;
import com.ccj.campus.chat.mapper.NoticeReadMapper;
import com.ccj.campus.chat.mapper.SysUserClassRelMapper;
import com.ccj.campus.chat.mapper.SysUserMapper;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.websocket.OnlineUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final OnlineUserService onlineUserService;

    // 新增
    private final SysUserMapper sysUserMapper;
    private final SysUserClassRelMapper userClassRelMapper;
    private final CourseMapper courseMapper;
    private final CourseClassRelMapper courseClassRelMapper;

    private boolean isAdmin(LoginUser user) {
        if (user == null || user.getRole() == null) return false;
        int role = user.getRole();
        return role == 0;
    }

    /**
     * 发布通知（仅管理员）
     */
    @PostMapping("/publish")
    public R<Notice> publish(@RequestBody @Valid NoticeReq req) {
        if (!isAdmin(LoginUser.current())) {
            return R.fail(ResultCode.FORBIDDEN);
        }

        Notice n = new Notice();
        n.setTitle(req.getTitle());
        n.setContent(req.getContent());
        n.setPublisherId(LoginUser.currentUid());
        n.setScopeType(req.getScopeType());
        n.setScopeData(req.getScopeData() == null ? new HashMap<>() : req.getScopeData());
        n.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(n);

        pushNoticeByScope(n);

        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "notice");
        evt.put("noticeId", n.getId());
        evt.put("title", n.getTitle());
        onlineUserService.broadcast("/topic/notice", evt);

        return R.ok(n);
    }

    private void pushNoticeByScope(Notice n) {
        Set<Long> targetUids = resolveNoticeTargetUids(n);
        if (targetUids.isEmpty()) return;

        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "notice");
        evt.put("noticeId", n.getId());
        evt.put("title", n.getTitle());
        evt.put("scopeType", n.getScopeType());

        for (Long uid : targetUids) {
            onlineUserService.push(uid, "/queue/messages", evt);
        }
    }

    private Set<Long> resolveNoticeTargetUids(Notice n) {
        Set<Long> uids = new HashSet<>();
        Integer scopeType = n.getScopeType();
        Map<String, Object> scopeData = n.getScopeData() == null ? Collections.emptyMap() : n.getScopeData();

        // 全量
        if (Objects.equals(scopeType, Notice.SCOPE_ALL)) {
            List<SysUser> users = sysUserMapper.selectList(new QueryWrapper<SysUser>().select("id"));
            for (SysUser u : users) {
                if (u != null && u.getId() != null) uids.add(u.getId());
            }
            return uids;
        }

        // 班级
        if (Objects.equals(scopeType, Notice.SCOPE_CLASS)) {
            Set<Long> classIds = collectLongIds(scopeData, "classId", "classIds", "id", "ids");
            for (Long classId : classIds) {
                List<Long> oneClassUsers = userClassRelMapper.listUserIdsByClass(classId);
                if (oneClassUsers != null) uids.addAll(oneClassUsers);
            }
            return uids;
        }

        return uids;
    }

    @GetMapping("/{id}")
    public R<Notice> detail(@PathVariable Long id) {
        return R.ok(noticeMapper.selectById(id));
    }

    /**
     * 查询通知列表：
     * - 管理员：全部
     * - 教师：教学班级 + 个人 + 院系 + 全量
     * - 学生：本人班级 + 个人 + 院系 + 全量
     */
    @GetMapping("/list")
    public R<List<Notice>> list(@RequestParam(defaultValue = "30") int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int fetchSize = Math.max(100, safeSize * 5);

        List<Notice> list = noticeMapper.selectList(
                new QueryWrapper<Notice>().orderByDesc("create_time").last("LIMIT " + fetchSize)
        );

        LoginUser loginUser = LoginUser.current();
        if (isAdmin(loginUser)) {
            return R.ok(list.size() <= safeSize ? list : list.subList(0, safeSize));
        }

        Long uid = loginUser.getUid();
        Long deptId = resolveDepartmentId(uid);
        Set<Long> classIds = resolveVisibleClassIds(loginUser);

        List<Notice> filtered = new ArrayList<>();
        for (Notice n : list) {
            if (isNoticeVisible(n, uid, deptId, classIds)) {
                filtered.add(n);
            }
            if (filtered.size() >= safeSize) break;
        }
        return R.ok(filtered);
    }

    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        noticeReadMapper.insertIgnore(id, LoginUser.currentUid());
        return R.ok();
    }

    @GetMapping("/{id}/readCount")
    public R<Integer> readCount(@PathVariable Long id) {
        return R.ok(noticeReadMapper.countByNotice(id));
    }

    @Data
    static class NoticeReq {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        @NotNull
        private Integer scopeType;
        private Map<String, Object> scopeData;
    }

    private Long resolveDepartmentId(Long uid) {
        SysUser user = sysUserMapper.selectById(uid);
        return user == null ? null : user.getDepartmentId();
    }

    private Set<Long> resolveVisibleClassIds(LoginUser loginUser) {
        if (loginUser.isStudent()) {
            List<Long> classIds = userClassRelMapper.listClassIdsByUser(loginUser.getUid());
            return classIds == null ? Collections.emptySet() : new HashSet<>(classIds);
        }

        if (loginUser.isTeacher()) {
            List<Course> courses = courseMapper.listByTeacher(loginUser.getUid());
            if (courses == null || courses.isEmpty()) return Collections.emptySet();

            Set<Long> ids = new HashSet<>();
            for (Course c : courses) {
                if (c == null || c.getId() == null) continue;
                List<Long> classIds = courseClassRelMapper.listClassIdsByCourse(c.getId());
                if (classIds != null) ids.addAll(classIds);
            }
            return ids;
        }

        return Collections.emptySet();
    }

    private boolean isNoticeVisible(Notice n, Long uid, Long deptId, Set<Long> classIds) {
        if (n == null) return false;
        Integer scopeType = n.getScopeType();
        if (Objects.equals(scopeType, Notice.SCOPE_ALL)) return true;

        Map<String, Object> scopeData = n.getScopeData() == null ? Collections.emptyMap() : n.getScopeData();

        if (Objects.equals(scopeType, Notice.SCOPE_PERSONAL)) {
            Set<Long> targetUserIds = collectLongIds(scopeData, "userId", "targetUserId", "uid", "userIds", "targetUserIds");
            return targetUserIds.contains(uid);
        }

        if (Objects.equals(scopeType, Notice.SCOPE_CLASS)) {
            if (classIds == null || classIds.isEmpty()) return false;
            Set<Long> targetClassIds = collectLongIds(scopeData, "classId", "classIds", "id", "ids");
            if (targetClassIds.isEmpty()) return false;
            for (Long classId : targetClassIds) {
                if (classIds.contains(classId)) return true;
            }
            return false;
        }

        if (Objects.equals(scopeType, Notice.SCOPE_DEPARTMENT)) {
            if (deptId == null) return false;
            Set<Long> targetDeptIds = collectLongIds(scopeData, "departmentId", "departmentIds", "deptId", "deptIds", "id", "ids");
            return targetDeptIds.contains(deptId);
        }

        return false;
    }

    private Set<Long> collectLongIds(Map<String, Object> data, String... keys) {
        if (data == null || data.isEmpty() || keys == null || keys.length == 0) return Collections.emptySet();
        Set<Long> result = new HashSet<>();
        for (String key : keys) {
            addValueAsIds(result, data.get(key));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addValueAsIds(Set<Long> sink, Object value) {
        if (sink == null || value == null) return;

        if (value instanceof Number) {
            sink.add(((Number) value).longValue());
            return;
        }

        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) return;
            if (text.contains(",")) {
                String[] arr = text.split(",");
                for (String part : arr) {
                    try {
                        sink.add(Long.parseLong(part.trim()));
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
            try {
                sink.add(Long.parseLong(text));
            } catch (Exception ignored) {
            }
            return;
        }

        if (value instanceof Collection<?>) {
            for (Object item : ((Collection<Object>) value)) {
                addValueAsIds(sink, item);
            }
            return;
        }

        if (value instanceof Map<?, ?>) {
            Map<Object, Object> m = (Map<Object, Object>) value;
            addValueAsIds(sink, m.get("id"));
            addValueAsIds(sink, m.get("value"));
        }
    }
}