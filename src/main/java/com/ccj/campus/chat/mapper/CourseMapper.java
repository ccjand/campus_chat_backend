package com.ccj.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccj.campus.chat.entity.Course;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CourseMapper extends BaseMapper<Course> {

    /** 查询教师负责的课程 */
    List<Course> listByTeacher(@Param("teacherId") Long teacherId);
}