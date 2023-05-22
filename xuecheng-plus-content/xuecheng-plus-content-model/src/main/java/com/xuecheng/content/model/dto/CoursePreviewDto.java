package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseTeacher;
import lombok.Data;

import java.util.List;

/**
 * 课程预览模型类
 */
@Data
public class CoursePreviewDto {

    //课程的基本信息
    //课程的营销信息
    private CourseBaseInfoDto courseBase;

    //课程的计划信息
    private List<TeachplanDto> teachplans;


    //课程的师资信息
    private List<CourseTeacher> teachers;
}
