package com.xuecheng.content;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CourseTeacherServiceTest {
    @Autowired
    private CourseTeacherService courseTeacherService;

    @Test
    void listTest() {
        Long courseId = 72L;
        List<CourseTeacher> list = courseTeacherService.list(courseId);
        System.out.println(list);
    }

    @Test
    void addAndUpdateTeacherTest() {
        CourseTeacher courseTeacher = new CourseTeacher();
        courseTeacher.setCourseId(75L);
        courseTeacher.setTeacherName("王老师");
        courseTeacher.setPosition("教师职位");
        courseTeacher.setIntroduction("教师简介");
        CourseTeacher addCourseTeacherResult = courseTeacherService.addAndUpdateTeacher(courseTeacher);
        System.out.println("addCourseTeacherResult = " + addCourseTeacherResult);

        courseTeacher.setTeacherName("李老师");
        courseTeacher.setId(addCourseTeacherResult.getId());
        CourseTeacher updateCourseTeacherResult = courseTeacherService.addAndUpdateTeacher(courseTeacher);
        System.out.println("updateCourseTeacherResult = " + updateCourseTeacherResult);

    }
}
