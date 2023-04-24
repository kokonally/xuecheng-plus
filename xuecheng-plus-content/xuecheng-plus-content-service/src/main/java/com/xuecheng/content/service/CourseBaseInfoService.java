package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * 课程基本信息业务接口
 */
public interface CourseBaseInfoService {
    /**
     * 分页查询
     * @param pageParams 页码参数
     * @param queryCourseParamsDto 查询条件
     * @return pageResult
     */
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto);

    /**
     * 新增课程
     * @param companyId 机构id
     * @return 课程添加成功的详细信息
     */
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto);
}
