package com.ccj.campus.chat.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysUserClassRelMapper {

    /** 绑定用户到班级 */
    int insertRel(@Param("userId") Long userId, @Param("classId") Long classId);

    /** 解绑 */
    int deleteRel(@Param("userId") Long userId, @Param("classId") Long classId);

    /** 查询用户所属班级 id 列表 */
    List<Long> listClassIdsByUser(@Param("userId") Long userId);

    /** 查询班级下的用户 id 列表 */
    List<Long> listUserIdsByClass(@Param("classId") Long classId);
}