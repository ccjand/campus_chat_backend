package com.ccj.campus.chat.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.UserLoginResp;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.mapper.SysUserMapper;
import com.ccj.campus.chat.security.JwtAuthenticationFilter;
import com.ccj.campus.chat.service.UserService;
import com.ccj.campus.chat.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户业务实现。对齐论文 4.4 + 6.2：
 *   - BCrypt 哈希校验（论文：密码以哈希形式存储）
 *   - 连续 5 次错误密码锁定 5 分钟（论文 6.2 测试用例）
 *   - 登出：token 加入 Redis 黑名单
 *   - 脱敏：Hutool DesensitizedUtil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String LOGIN_FAIL_KEY = "login:fail:";
    private static final String LOGIN_LOCK_KEY = "login:lock:";

    private final SysUserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;

    @Value("${campus.security.login-fail-lock-threshold:5}")
    private int lockThreshold;

    @Value("${campus.security.login-fail-lock-minutes:5}")
    private int lockMinutes;

    @Override
    public UserLoginResp login(String accountNumber, String password) {
        // 1. 锁定检查
        String lockKey = LOGIN_LOCK_KEY + accountNumber;
        if (Boolean.TRUE.equals(redis.hasKey(lockKey))) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED,
                    "账号已锁定，请 " + lockMinutes + " 分钟后重试");
        }

        // 2. 查用户
        SysUser user = userMapper.selectByAccount(accountNumber);
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号不存在");
        }

        // 3. 密码校验
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleLoginFail(accountNumber);
            throw new BusinessException(ResultCode.BAD_REQUEST, "密码错误");
        }

        // 4. 登录成功，清除失败计数
        redis.delete(LOGIN_FAIL_KEY + accountNumber);

        // 5. 颁发 JWT（对齐论文 3.2：角色信息写入载荷）
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        return UserLoginResp.builder()
                .uid(user.getId())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .token(token)
                .build();
    }

    @Override
    public void logout(Long uid, String token) {
        // 对齐论文摘要："JWT 无状态认证配合 Redis 黑名单实现主动令牌失效"
        redis.opsForValue().set(
                JwtAuthenticationFilter.BLACKLIST_KEY_PREFIX + token,
                "1",
                7, TimeUnit.DAYS);
    }

    @Override
    public SysUser getById(Long uid) {
        SysUser u = userMapper.selectById(uid);
        if (u == null) return null;
        // 对齐论文 4.4："对手机号等敏感字段进行了脱敏处理"
        u.setPhone(DesensitizedUtil.mobilePhone(u.getPhone()));
        u.setEmail(DesensitizedUtil.email(u.getEmail()));
        u.setPasswordHash(null);  // 永远不对外暴露
        return u;
    }

    // ==================== 内部方法 ====================

    /** 对齐论文 6.2 测试用例："连续 5 次错误密码输入后账号锁定 5 分钟" */
    private void handleLoginFail(String accountNumber) {
        String failKey = LOGIN_FAIL_KEY + accountNumber;
        Long count = redis.opsForValue().increment(failKey);
        redis.expire(failKey, lockMinutes, TimeUnit.MINUTES);

        if (count != null && count >= lockThreshold) {
            redis.opsForValue().set(LOGIN_LOCK_KEY + accountNumber, "1",
                    lockMinutes, TimeUnit.MINUTES);
            redis.delete(failKey);
            log.warn("账号 {} 连续登录失败 {} 次，锁定 {} 分钟", accountNumber, count, lockMinutes);
        }
    }
}