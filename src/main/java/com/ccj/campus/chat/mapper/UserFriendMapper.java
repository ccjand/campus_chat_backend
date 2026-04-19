package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.UserFriend;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserFriendMapper extends BaseMapper<UserFriend> {

    /** 查询用户的好友列表（带好友用户信息） */
    List<UserFriend> listByUser(@Param("userId") Long userId);

    /** 查询两人之间的好友关系（双向任一方向即可） */
    UserFriend selectRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);
}