package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 课程教师业务
 */
public interface CourseTeacherService {

    public abstract List<CourseTeacher> list(Long courseId);

}
