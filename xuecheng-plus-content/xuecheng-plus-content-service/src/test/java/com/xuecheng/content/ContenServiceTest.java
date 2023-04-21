package com.xuecheng.content;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ContenServiceTest {
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;
    @Test
    public void testCourseBaseMapper() {
        CourseBase courseBase = courseBaseMapper.selectById(1);
        Assertions.assertNotNull(courseBase);
        System.out.println("courseBase = " + courseBase);

        QueryCourseParamsDto queryCourseParamsDto = new QueryCourseParamsDto();
        queryCourseParamsDto.setCourseName("java");

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //根据名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName, queryCourseParamsDto.getCourseName());

        //根据课程的审核状态进行查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());

        //发布状态条件
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());

        //分页查询条件
        Page<CourseBase> page = new Page<>(1, 2);

        Page<CourseBase> courseBasePage = courseBaseMapper.selectPage(page, queryWrapper);
        PageResult<CourseBase> pageResult =
                new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());

        System.out.println("pageResult = " + pageResult);
    }
}
