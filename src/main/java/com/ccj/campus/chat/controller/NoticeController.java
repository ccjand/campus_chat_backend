package com.ccj.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.Notice;
import com.ccj.campus.chat.mapper.NoticeMapper;
import com.ccj.campus.chat.mapper.NoticeReadMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知接口。对齐论文 3.2：
 *   "系统管理员或教师可向指定用户群体发布通知，
 *    支持全量推送与定向推送（指定院系、班级或个人）。"
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper noticeReadMapper;
    private final OnlineUserService onlineUserService;

    /** 发布通知（教师 / 管理员） */
    @PreAuthorize("hasAnyAuthority('ROLE_TEACHER','ROLE_ADMIN')")
    @PostMapping("/publish")
    public R<Notice> publish(@RequestBody @Valid NoticeReq req) {
        Notice n = new Notice();
        n.setTitle(req.getTitle());
        n.setContent(req.getContent());
        n.setPublisherId(LoginUser.currentUid());
        n.setScopeType(req.getScopeType());
        n.setScopeData(req.getScopeData() == null ? new HashMap<>() : req.getScopeData());
        n.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(n);

        // 广播通知事件，前端收到后弹出
        Map<String, Object> evt = new HashMap<>();
        evt.put("event", "notice");
        evt.put("noticeId", n.getId());
        evt.put("title", n.getTitle());
        onlineUserService.broadcast("/topic/notice", evt);

        return R.ok(n);
    }

    /** 通知详情 */
    @GetMapping("/{id}")
    public R<Notice> detail(@PathVariable Long id) {
        return R.ok(noticeMapper.selectById(id));
    }

    /** 查询通知列表 */
    @GetMapping("/list")
    public R<List<Notice>> list(@RequestParam(defaultValue = "30") int size) {
        List<Notice> list = noticeMapper.selectList(
                new QueryWrapper<Notice>().orderByDesc("create_time").last("LIMIT " + size));
        return R.ok(list);
    }

    /** 标记通知已读 */
    @PostMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        noticeReadMapper.insertIgnore(id, LoginUser.currentUid());
        return R.ok();
    }

    /** 查询某通知已读人数 */
    @GetMapping("/{id}/readCount")
    public R<Integer> readCount(@PathVariable Long id) {
        return R.ok(noticeReadMapper.countByNotice(id));
    }

    @Data
    static class NoticeReq {
        @NotBlank private String title;
        @NotBlank private String content;
        @NotNull  private Integer scopeType;
        private Map<String, Object> scopeData;
    }
}