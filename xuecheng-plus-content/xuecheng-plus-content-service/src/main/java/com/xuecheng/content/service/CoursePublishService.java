package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.CoursePreviewDto;

/**
 * 课程发布相关业务
 */
public interface CoursePublishService {

    /**
     * 获取课程浏览信息
     * @param courseId 课程id
     * @return com.xuecheng.content.model.dto.CoursePreviewDto
     */
    CoursePreviewDto getCoursePreviewInfo(Long courseId);
}
