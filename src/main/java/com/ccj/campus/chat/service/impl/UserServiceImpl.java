package com.ccj.campus.chat.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccj.campus.chat.common.BusinessException;
import com.ccj.campus.chat.common.ResultCode;
import com.ccj.campus.chat.dto.UserLoginResp;
import com.ccj.campus.chat.dto.UserSearchVO;
import com.ccj.campus.chat.entity.SysDepartment;
import com.ccj.campus.chat.entity.SysUser;
import com.ccj.campus.chat.mapper.SysDepartmentMapper;
import com.ccj.campus.chat.mapper.SysUserMapper;
import com.ccj.campus.chat.security.JwtAuthenticationFilter;
import com.ccj.campus.chat.security.LoginUser;
import com.ccj.campus.chat.service.FriendService;
import com.ccj.campus.chat.service.UserService;
import com.ccj.campus.chat.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户业务实现。对齐论文 4.4 + 6.2：
 * - BCrypt 哈希校验（论文：密码以哈希形式存储）
 * - 连续 5 次错误密码锁定 5 分钟（论文 6.2 测试用例）
 * - 登出：token 加入 Redis 黑名单
 * - 脱敏：Hutool DesensitizedUtil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private static final String LOGIN_FAIL_KEY = "login:fail:";
    private static final String LOGIN_LOCK_KEY = "login:lock:";

    private final SysUserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final SysDepartmentMapper departmentMapper;
    private final FriendService friendService;

    @Value("${campus.security.login-fail-lock-threshold:5}")
    private int lockThreshold;

    @Value("${campus.security.login-fail-lock-minutes:5}")
    private int lockMinutes;

    @Override
    public List<UserSearchVO> getApprovers() {
        Long currentUid = LoginUser.currentUid();
        SysUser currentUser = userMapper.selectById(currentUid);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        Long departmentId = currentUser.getDepartmentId();
        if (departmentId == null) {
            return new ArrayList<>();
        }

        Integer currentRole = currentUser.getRole();
        Integer targetRole = null;

        // 学生 -> 选本学院辅导员
        if (currentRole != null && currentRole == SysUser.ROLE_STUDENT) {
            targetRole = SysUser.ROLE_COUNSELOR;
        }
        // 教师/辅导员 -> 选本学院院长
        else if (currentRole != null &&
                (currentRole == SysUser.ROLE_TEACHER || currentRole == SysUser.ROLE_COUNSELOR)) {
            targetRole = SysUser.ROLE_STAFF;
        }
        // 其他角色不返回审批人
        else {
            return new ArrayList<>();
        }

        List<SysUser> approvers = userMapper.selectList(new QueryWrapper<SysUser>()
                .eq("department_id", departmentId)
                .eq("role", targetRole)
                .eq("enabled", true));

        return approvers.stream().map(user -> {
            UserSearchVO vo = new UserSearchVO();
            vo.setUserId(user.getId());
            vo.setAccountNumber(user.getAccountNumber());
            vo.setName(user.getName());
            vo.setAvatar(user.getAvatar());
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    public void updatePasswordByAccount(String accountNumber, String newPassword) {
        // 1. 密码加密
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 2. 更新数据库
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getAccountNumber, accountNumber)
                .set(SysUser::getPasswordHash, encodedPassword);

        this.update(updateWrapper);
    }

    @Override
    public void updatePasswordById(Long userId, String newPassword) {
        // 1. 密码加密
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 2. 更新数据库
        LambdaUpdateWrapper<SysUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysUser::getId, userId) // 如果你的主键不是 getId，请换成 getUid 之类的
                .set(SysUser::getPasswordHash, encodedPassword);

        this.update(updateWrapper);
    }

    @Override
    public void updateUserInfo(SysUser user) {
        this.updateById(user);
    }

    @Override
    public SysUser getByAccountNumber(String accountNumber) {
        return userMapper.selectByAccount(accountNumber);
    }


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
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入正确的账号密码");
        }

        // 3. 密码校验
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleLoginFail(accountNumber);
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入正确的账号密码");
        }

        // 4. 登录成功，清除失败计数
        redis.delete(LOGIN_FAIL_KEY + accountNumber);

        // 5. 颁发 JWT（对齐论文 3.2：角色信息写入载荷）
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        // 6. 查院系名（供前端"我的"页面展示）
        String departmentName = null;
        if (user.getDepartmentId() != null) {
            SysDepartment dept = departmentMapper.selectById(user.getDepartmentId());
            if (dept != null) departmentName = dept.getName();
        }

        return UserLoginResp.builder()
                .uid(user.getId())
                .name(user.getName())
                .accountNumber(user.getAccountNumber())
                .departmentName(departmentName)
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

    /**
     * 对齐论文 6.2 测试用例："连续 5 次错误密码输入后账号锁定 5 分钟"
     */
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

    @Override
    public List<UserSearchVO> searchUsers(String keyword, Long currentUid) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 模糊查询（学号或姓名），只查启用用户，排除自己
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.and(w -> w.like("account_number", keyword)
                        .or().like("name", keyword))
                .eq("enabled", true)
                .ne("id", currentUid)
                .last("LIMIT 20");  // 限制返回数量，防止全表扫描

        List<SysUser> users = userMapper.selectList(qw);

        // 2. 组装 VO，附加好友/黑名单状态
        return users.stream().map(u -> {
            // 查院系名称
            String deptName = null;
            if (u.getDepartmentId() != null) {
                SysDepartment dept = departmentMapper.selectById(u.getDepartmentId());
                if (dept != null) deptName = dept.getName();
            }
            return UserSearchVO.builder()
                    .userId(u.getId())
                    .accountNumber(u.getAccountNumber())
                    .name(u.getName())
                    .role(u.getRole())
                    .avatar(u.getAvatar())
                    .departmentName(deptName)
                    .isFriend(friendService.isFriend(currentUid, u.getId()))
                    .isBlocked(friendService.isBlocked(currentUid, u.getId()))
                    .build();
        }).collect(Collectors.toList());
    }
}