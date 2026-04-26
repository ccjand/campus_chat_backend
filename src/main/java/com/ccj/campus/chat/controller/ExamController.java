package com.ccj.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.Exam;
import com.ccj.campus.chat.mapper.ExamMapper;
import com.ccj.campus.chat.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试接口。对齐论文 3.2：
 * "学生可在应用内查询本人的考试安排，避免遗漏考试时间与地点。"
 */
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamMapper examMapper;

    /**
     * 学生：查询我的考试
     */
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @GetMapping("/mine")
    public R<List<Exam>> mine() {
        return R.ok(examMapper.listByStudent(LoginUser.currentUid()));
    }

    /**
     * 管理员：录入考试
     */
    @PostMapping("/create")
    public R<Exam> create(@RequestBody Exam exam) {
        exam.setCreateTime(LocalDateTime.now());
        examMapper.insert(exam);
        return R.ok(exam);
    }

    /**
     * 管理员：查询考试列表（用于“考试列表”tag）
     */
    @GetMapping("/list")
    public R<List<Exam>> list(@RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.max(1, Math.min(size, 200));
        List<Exam> list = examMapper.selectList(
                new QueryWrapper<Exam>()
                        .orderByDesc("create_time")
                        .last("LIMIT " + safeSize)
        );
        return R.ok(list);
    }
}