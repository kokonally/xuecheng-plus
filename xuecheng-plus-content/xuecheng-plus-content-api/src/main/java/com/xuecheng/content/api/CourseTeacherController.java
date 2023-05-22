package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 师资管理相关接口
 */
@RestController
@Api(value = "师资管理相关接口", tags = "师资管理相关接口")
public class CourseTeacherController {
    @Autowired
    private CourseTeacherService courseTeacherService;

    @ApiOperation("查询教师接口")
    @GetMapping("/courseTeacher/list/{courseId}")
    public List<CourseTeacher> list(@PathVariable("courseId") Long courseId) {
        return courseTeacherService.list(courseId);
    }

    @ApiOperation("添加和修改教师")
    @PostMapping("/courseTeacher")
    public CourseTeacher addAndUpdateCourseTeacher(@RequestBody CourseTeacher courseTeacher) {
        return courseTeacherService.addAndUpdateTeacher(courseTeacher);
    }

    @ApiOperation("删除教师")
    @DeleteMapping("/courseTeacher/course/{courseId}/{courseTeacherId}")
    public void deleteCourseTeacher(@PathVariable("courseId") Long courseId,
                                    @PathVariable("courseTeacherId") Long courseTeacherId) {
        courseTeacherService.deleteTeacher(courseId, courseTeacherId);
    }
}
