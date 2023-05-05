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
}
