package com.ccj.campus.chat.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseClassRelMapper {

    int insertRel(@Param("courseId") Long courseId, @Param("classId") Long classId);

    int deleteRel(@Param("courseId") Long courseId, @Param("classId") Long classId);

    List<Long> listClassIdsByCourse(@Param("courseId") Long courseId);
}