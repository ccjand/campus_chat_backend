package com.ccj.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.*;
import com.ccj.campus.chat.mapper.*;
import com.ccj.campus.chat.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员后台接口。对齐论文 3.4：
 *   "管理员操作通过专属的管理后台接口完成，
 *    与师生端接口在路径前缀上完全隔离。"
 *
 * 路径：/admin/**  在 SecurityConfig 中限制 ROLE_ADMIN
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final SysDepartmentMapper departmentMapper;
    private final SysClassMapper classMapper;
    private final SysUserMapper userMapper;
    private final CourseMapper courseMapper;
    private final ExamStudentRelMapper examStudentRelMapper;
    private final SysUserClassRelMapper userClassRelMapper;
    private final CourseClassRelMapper courseClassRelMapper;
    private final PasswordEncoder passwordEncoder;

    // ==================== 院系管理 ====================

    @PostMapping("/department/create")
    public R<SysDepartment> createDept(@RequestBody @Valid DeptReq req) {
        SysDepartment d = new SysDepartment();
        d.setName(req.getName());
        d.setCode(req.getCode());
        d.setCreateTime(LocalDateTime.now());
        departmentMapper.insert(d);
        return R.ok(d);
    }

    @GetMapping("/department/list")
    public R<List<SysDepartment>> listDepts() {
        return R.ok(departmentMapper.selectList(null));
    }

    @DeleteMapping("/department/{id}")
    public R<Void> deleteDept(@PathVariable Long id) {
        departmentMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 班级管理 ====================

    @PostMapping("/class/create")
    public R<SysClass> createClass(@RequestBody @Valid ClassReq req) {
        SysClass c = new SysClass();
        c.setDepartmentId(req.getDepartmentId());
        c.setName(req.getName());
        c.setGrade(req.getGrade());
        c.setCreateTime(LocalDateTime.now());
        classMapper.insert(c);
        return R.ok(c);
    }

    @GetMapping("/class/list")
    public R<List<SysClass>> listClasses(@RequestParam(required = false) Long departmentId) {
        QueryWrapper<SysClass> qw = new QueryWrapper<>();
        if (departmentId != null) qw.eq("department_id", departmentId);
        return R.ok(classMapper.selectList(qw));
    }

    @DeleteMapping("/class/{id}")
    public R<Void> deleteClass(@PathVariable Long id) {
        classMapper.deleteById(id);
        return R.ok();
    }

    // ==================== 用户管理 ====================

    /** 创建用户（论文 3.4：用户账号的创建与角色分配） */
    @PostMapping("/user/create")
    public R<SysUser> createUser(@RequestBody @Valid CreateUserReq req) {
        SysUser u = new SysUser();
        u.setAccountNumber(req.getAccountNumber());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setName(req.getName());
        u.setRole(req.getRole());
        u.setDepartmentId(req.getDepartmentId());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setEnabled(true);
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);
        u.setPasswordHash(null); // 不返回密码
        return R.ok(u);
    }

    /** 停用/启用用户（论文 3.4：用户账号的停用） */
    @PostMapping("/user/{id}/toggle")
    public R<Void> toggleUser(@PathVariable Long id, @RequestBody ToggleReq req) {
        SysUser u = userMapper.selectById(id);
        if (u != null) {
            u.setEnabled(req.getEnabled());
            u.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(u);
        }
        return R.ok();
    }

    /** 用户列表 */
    @GetMapping("/user/list")
    public R<List<SysUser>> listUsers(@RequestParam(required = false) Integer role) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (role != null) qw.eq("role", role);
        qw.orderByDesc("create_time");
        List<SysUser> list = userMapper.selectList(qw);
        list.forEach(u -> u.setPasswordHash(null));
        return R.ok(list);
    }

    // ==================== 课程管理 ====================

    @PostMapping("/course/create")
    public R<Course> createCourse(@RequestBody @Valid CourseReq req) {
        Course c = new Course();
        c.setName(req.getName());
        c.setTeacherId(req.getTeacherId());
        c.setCreateTime(LocalDateTime.now());
        courseMapper.insert(c);
        return R.ok(c);
    }

    @GetMapping("/course/list")
    public R<List<Course>> listCourses() {
        return R.ok(courseMapper.selectList(null));
    }

    // ==================== 考试学生绑定 ====================

    /** 批量绑定学生到考试 */
    @PostMapping("/exam/{examId}/bindStudents")
    public R<Void> bindStudents(@PathVariable Long examId, @RequestBody List<ExamStudentRel> list) {
        if (list != null && !list.isEmpty()) {
            examStudentRelMapper.batchInsert(examId, list);
        }
        return R.ok();
    }

    // ==================== 用户-班级绑定 ====================

    /** 绑定用户到班级 */
    @PostMapping("/user/{userId}/bindClass/{classId}")
    public R<Void> bindUserClass(@PathVariable Long userId, @PathVariable Long classId) {
        userClassRelMapper.insertRel(userId, classId);
        return R.ok();
    }

    /** 解绑用户与班级 */
    @DeleteMapping("/user/{userId}/unbindClass/{classId}")
    public R<Void> unbindUserClass(@PathVariable Long userId, @PathVariable Long classId) {
        userClassRelMapper.deleteRel(userId, classId);
        return R.ok();
    }

    /** 查询用户所属班级 */
    @GetMapping("/user/{userId}/classes")
    public R<List<Long>> userClasses(@PathVariable Long userId) {
        return R.ok(userClassRelMapper.listClassIdsByUser(userId));
    }

    /** 查询班级下的用户 */
    @GetMapping("/class/{classId}/users")
    public R<List<Long>> classUsers(@PathVariable Long classId) {
        return R.ok(userClassRelMapper.listUserIdsByClass(classId));
    }

    // ==================== 课程-班级绑定 ====================

    /** 绑定课程到班级 */
    @PostMapping("/course/{courseId}/bindClass/{classId}")
    public R<Void> bindCourseClass(@PathVariable Long courseId, @PathVariable Long classId) {
        courseClassRelMapper.insertRel(courseId, classId);
        return R.ok();
    }

    /** 解绑课程与班级 */
    @DeleteMapping("/course/{courseId}/unbindClass/{classId}")
    public R<Void> unbindCourseClass(@PathVariable Long courseId, @PathVariable Long classId) {
        courseClassRelMapper.deleteRel(courseId, classId);
        return R.ok();
    }

    /** 查询课程关联的班级 */
    @GetMapping("/course/{courseId}/classes")
    public R<List<Long>> courseClasses(@PathVariable Long courseId) {
        return R.ok(courseClassRelMapper.listClassIdsByCourse(courseId));
    }

    // ==================== 内部 DTO ====================

    @Data static class DeptReq        { @NotBlank private String name; @NotBlank private String code; }
    @Data static class ClassReq       { @NotNull private Long departmentId; @NotBlank private String name; @NotNull private Integer grade; }
    @Data static class CreateUserReq  { @NotBlank private String accountNumber; @NotBlank private String password; @NotBlank private String name; @NotNull private Integer role; private Long departmentId; private String phone; private String email; }
    @Data static class ToggleReq      { @NotNull private Boolean enabled; }
    @Data static class CourseReq      { @NotBlank private String name; @NotNull private Long teacherId; }
}