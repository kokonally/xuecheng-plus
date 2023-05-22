package com.xuecheng.content.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseTeacherService courseTeacherService;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //1.查询基本信息和营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);

        //2.查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);

        return coursePreviewDto;
    }

    @Override
    @Transactional
    public void commitAudit(Long courseId, Long companyId) {
        //查询到课程基本信息、营销信息、课程计划信息插入到课程预发布表

        //1.判断课程状态是否为已提交，已提交不能重复提交
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.getCourseBaseInfo(courseId);
        if (courseBaseInfoDto == null) {
            XueChengPlusException.cast("课程信息不存在");
        }
        String auditStatus = courseBaseInfoDto.getAuditStatus();  //课程审核状态
        if ("202003".equals(auditStatus)) {
            //审核已提交，不允许重复提交
            XueChengPlusException.cast("课程已提交，请等待审核");
        }
        //2.课程的图片、计划信息没有填写不允许提交
        String pic = courseBaseInfoDto.getPic();
        if (StrUtil.isEmpty(pic)) {
            XueChengPlusException.cast("请上传课程图片");
        }
        List<TeachplanDto> teachplanDtos = teachplanService.findTeachplanTree(courseId);
        if (teachplanDtos.isEmpty()) {
            XueChengPlusException.cast("请上传课程计划");
        }

        //3.将数据写入到预发布表
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfoDto, coursePublishPre);

        //本机构只能校验本机构课程
        Long companyIdFromDB = courseBaseInfoDto.getCompanyId();
        if (!companyIdFromDB.equals(companyId)) {
            XueChengPlusException.cast("你无权操作其他机构的课程");
        }

        //设置机构id
        coursePublishPre.setCompanyId(companyId);

        //营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarket_json = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarket_json);

        //课程计划信息
        String teachplan_json = JSON.toJSONString(teachplanDtos);
        coursePublishPre.setTeachplan(teachplan_json);

        //设置教师信息
        List<CourseTeacher> teachers = courseTeacherService.list(courseId);
        String teachers_json = JSON.toJSONString(teachers);
        coursePublishPre.setTeachers(teachers_json);

        //设置状态为已提交
        coursePublishPre.setStatus("202003");

        //提交时间
        coursePublishPre.setAuditDate(LocalDateTime.now());

        //4.插入
        if (coursePublishPreMapper.selectCount(new LambdaQueryWrapper<CoursePublishPre>()
                .eq(CoursePublishPre::getId, courseId)) == 1) {
            //预发布表里有数据
            coursePublishPreMapper.updateById(coursePublishPre);
        } else {
            coursePublishPreMapper.insert(coursePublishPre);
        }

        //5.更新课程基本表的状态
        courseBaseInfoDto.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBaseInfoDto);
    }
}
