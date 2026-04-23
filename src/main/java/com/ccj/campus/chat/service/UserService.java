package com.ccj.campus.chat.service;

import com.ccj.campus.chat.dto.UserLoginResp;
import com.ccj.campus.chat.dto.UserSearchVO;
import com.ccj.campus.chat.entity.SysUser;

import java.util.List;

/**
 * 用户与认证业务。对齐论文 3.2 + 4.4：
 *   - 学号/工号登录，JWT 令牌颁发
 *   - 连续 5 次错误密码锁定 5 分钟
 *   - 登出：Redis 黑名单主动吊销令牌
 *   - 敏感字段 Hutool 脱敏
 */
public interface UserService {

    /** 登录 */
    UserLoginResp login(String accountNumber, String password);

    /** 登出 */
    void logout(Long uid, String token);

    /** 按 id 查询（对外接口返回脱敏后的信息） */
    SysUser getById(Long uid);

    /** 按关键词搜索用户（学号/姓名模糊匹配） */
    List<UserSearchVO> searchUsers(String keyword, Long currentUid);


    /** 获取所有可选审批人（教师或辅导员） */
    List<UserSearchVO> getApprovers();
}