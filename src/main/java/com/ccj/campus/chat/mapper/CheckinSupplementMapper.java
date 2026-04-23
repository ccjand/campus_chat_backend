package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.CheckinSupplement;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 补签 Mapper。
 * 基础 CRUD 由 BaseMapper 提供；自定义 JOIN 查询由 CheckinSupplementMapper.xml 实现。
 */
public interface CheckinSupplementMapper extends BaseMapper<CheckinSupplement> {

    /**
     * 列出某老师发起的所有签到被提交的补签申请。
     * 返回字段包含：补签信息 + 申请学生基础信息 + 所属签到的课程与标题。
     *
     * @param teacherId    发起老师 uid
     * @param pendingOnly  true=只返回 status=0 (待审批)
     */
    List<Map<String, Object>> listByTeacher(@Param("teacherId") Long teacherId,
                                             @Param("pendingOnly") boolean pendingOnly);
}