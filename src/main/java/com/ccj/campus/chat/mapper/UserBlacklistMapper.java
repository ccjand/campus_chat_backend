package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.UserBlacklist;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {

    /** 查询用户的黑名单列表 */
    List<UserBlacklist> listByUser(@Param("userId") Long userId);

    /** 判断是否在黑名单中 */
    UserBlacklist selectByUserAndTarget(@Param("userId") Long userId, @Param("targetId") Long targetId);
}