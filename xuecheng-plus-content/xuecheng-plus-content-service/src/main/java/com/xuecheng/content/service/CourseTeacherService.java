package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

import java.util.List;

/**
 * 课程教师业务
 */
public interface CourseTeacherService {
    /**
     * 根据课程id查询课程教师
     * @param courseId 课程id
     * @return
     */
    public abstract List<CourseTeacher> list(Long courseId);

    /**
     * 添加和修改教师
     * @param courseTeacher 教师信息
     * @return
     */
    CourseTeacher addAndUpdateTeacher(CourseTeacher courseTeacher);

    /**
     * 删除教师信息
     * @param courseId 课程id
     * @param teacherId 教师id
     */
    void deleteTeacher(Long courseId, Long teacherId);

}
