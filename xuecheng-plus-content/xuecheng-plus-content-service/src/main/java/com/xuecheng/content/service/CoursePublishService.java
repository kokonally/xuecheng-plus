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

    /**
     * 提交课程审核
     * @param courseId 需要提交审核的课程id
     */
    void commitAudit(Long courseId, Long companyId);
}
