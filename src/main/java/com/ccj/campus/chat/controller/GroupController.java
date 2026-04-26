package com.ccj.campus.chat.controller;

import com.ccj.campus.chat.common.R;
import com.ccj.campus.chat.entity.ChatGroup;
import com.ccj.campus.chat.entity.ChatGroupMember;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.GroupService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 群组接口。对齐论文 3.2 + 4.3 群组组。
 */
@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @GetMapping("/create/candidates")
    public R<Map<String, Object>> createCandidates() {
        return R.ok(groupService.getCreateCandidates(LoginUser.currentUid()));
    }

    @PostMapping("/create")
    public R<ChatGroup> create(@RequestBody @Valid CreateGroupReq req) {
        return R.ok(groupService.create(
                LoginUser.currentUid(),
                req.getName(),
                req.getType(),
                req.getClassId(),
                req.getMemberIds()
        ));
    }

    @PostMapping("/{groupId}/member/add")
    public R<Void> addMember(@PathVariable Long groupId, @RequestBody UserIdReq req) {
        groupService.addMember(LoginUser.currentUid(), groupId, req.getUserId());
        return R.ok();
    }

    @PostMapping("/{groupId}/member/remove")
    public R<Void> removeMember(@PathVariable Long groupId, @RequestBody UserIdReq req) {
        groupService.removeMember(LoginUser.currentUid(), groupId, req.getUserId());
        return R.ok();
    }

    @PostMapping("/{groupId}/admin")
    public R<Void> setAdmin(@PathVariable Long groupId, @RequestBody AdminReq req) {
        groupService.setAdmin(LoginUser.currentUid(), groupId, req.getUserId(), req.isAdmin());
        return R.ok();
    }

    @PostMapping("/{groupId}/transfer")
    public R<Void> transfer(@PathVariable Long groupId, @RequestBody UserIdReq req) {
        groupService.transferOwner(LoginUser.currentUid(), groupId, req.getUserId());
        return R.ok();
    }

    @PostMapping("/{groupId}/dissolve")
    public R<Void> dissolve(@PathVariable Long groupId) {
        groupService.dissolve(LoginUser.currentUid(), groupId);
        return R.ok();
    }

    @PostMapping("/{groupId}/announcement")
    public R<Void> announcement(@PathVariable Long groupId, @RequestBody AnnouncementReq req) {
        groupService.updateAnnouncement(LoginUser.currentUid(), groupId, req.getAnnouncement());
        return R.ok();
    }

    @GetMapping("/{groupId}/members")
    public R<List<ChatGroupMember>> members(@PathVariable Long groupId) {
        return R.ok(groupService.listMembers(groupId));
    }

    @Data
    static class CreateGroupReq {
        @NotBlank
        private String name;
        @NotNull
        private Integer type;
        private Long classId;
        private List<Long> memberIds;
    }

    @Data
    static class UserIdReq {
        @NotNull
        private Long userId;
    }

    @Data
    static class AdminReq {
        @NotNull
        private Long userId;
        private boolean admin;
    }

    @Data
    static class AnnouncementReq {
        private String announcement;
    }
}