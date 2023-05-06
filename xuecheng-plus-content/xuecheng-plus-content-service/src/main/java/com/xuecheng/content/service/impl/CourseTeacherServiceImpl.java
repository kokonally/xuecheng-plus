package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.ValidationGroups;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public List<CourseTeacher> list(Long courseId) {
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseTeacher::getCourseId, courseId);
        return courseTeacherMapper.selectList(queryWrapper);
    }

    @Override
    public CourseTeacher addAndUpdateTeacher(CourseTeacher courseTeacher) {
        //1.判断是新增还是修改
        if (courseTeacher.getId() == null) {
            //新增
            this.addCourseTeacher(courseTeacher);
        } else {
            //修改
            this.updateCourseTeacher(courseTeacher);
        }

        //2.查询操作
        return courseTeacherMapper.selectById(courseTeacher.getId());
    }

    private void addCourseTeacher(@Validated(ValidationGroups.Inster.class) CourseTeacher courseTeacher) {
        courseTeacherMapper.insert(courseTeacher);
    }

    private void updateCourseTeacher(@Validated(ValidationGroups.Update.class) CourseTeacher courseTeacher) {
        courseTeacherMapper.updateById(courseTeacher);
    }
}
