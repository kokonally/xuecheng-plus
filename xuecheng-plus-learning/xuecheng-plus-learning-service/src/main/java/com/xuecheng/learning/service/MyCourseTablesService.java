package com.xuecheng.learning.service;

import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;

/**
 * 选课相关操作
 */
public interface MyCourseTablesService {
    /**
     * 选课
     * @param userid 用户id
     * @param courseId 课程id
     * @return
     */
    XcChooseCourseDto addChooseCourse(String userid, Long courseId);

    /**
     * 获取学生的学习资格
     * @param userId 用户id
     * @param courseId 课程id
     * @return
     */
    XcCourseTablesDto getLearningStatus(String userId, Long courseId);

    /**
     * 保存选课成功
     * @param chooseCourseId
     * @return
     */
    public boolean saveChooseCourseSuccess(String chooseCourseId);
}
