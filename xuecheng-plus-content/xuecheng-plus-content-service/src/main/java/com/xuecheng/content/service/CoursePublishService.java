package com.xuecheng.content.service;


import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;

import java.io.File;

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

    /**
     * 课程发布
     * @param courseId 课程id
     */
    void coursepublish(Long companyId, Long courseId);

    /**
     * 生成指定课程的静态html
     * @param courseId 课程id
     * @return
     */
    File generateCourseHtml(Long courseId);

    /**
     * 上传课程html文件
     * @param courseId 课程id
     * @param file 文件
     */
    void uploadCourseHtml(Long courseId, File file);

    /**
     * 根据课程id查询发布课程信息
     * @param courseId 课程id
     * @return
     */
    CoursePublish getCoursePublish(Long courseId);
}
