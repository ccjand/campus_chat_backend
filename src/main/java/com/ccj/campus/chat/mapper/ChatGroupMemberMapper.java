package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.ChatGroupMember;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ChatGroupMemberMapper extends BaseMapper<ChatGroupMember> {

    /** 查询用户在某群的角色 */
    ChatGroupMember selectByGroupAndUser(@Param("groupId") Long groupId,
                                          @Param("userId") Long userId);

    /** 群成员 id 列表 */
    List<Long> listMemberUids(@Param("groupId") Long groupId);
}