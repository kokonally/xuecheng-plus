package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.*;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    private CourseBaseMapper courseBaseMapper;

    @Autowired
    private CourseMarketMapper courseMarketMapper;

    @Autowired
    private CourseCategoryMapper courseCategoryMapper;

    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //1.设置分页查询条件
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());

        //2.设置条件
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        //设置课程名称查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName, queryCourseParamsDto.getCourseName());

        //设置课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus, queryCourseParamsDto.getAuditStatus());

        //设置课程的发布状态进行查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getStatus, queryCourseParamsDto.getPublishStatus());

        //3.进行查询获得数据
        Page<CourseBase> courseBasePage = courseBaseMapper.selectPage(page, queryWrapper);
        List<CourseBase> courseBases = courseBasePage.getRecords();
        if (courseBases == null) {
            courseBases = Collections.emptyList();
        }

        //4.包装成Result
        return new PageResult<>(courseBases, courseBasePage.getTotal(),
                courseBasePage.getCurrent(), courseBasePage.getSize());
    }

    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto dto) {
        //1.参数合法性校验
        /*if (StringUtils.isBlank(dto.getName())) {
//            throw new RuntimeException("课程名称为空");
            XueChengPlusException.cast("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
//            throw new RuntimeException("课程分类为空");
            XueChengPlusException.cast("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
//            throw new RuntimeException("课程分类为空");
            XueChengPlusException.cast("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
//            throw new RuntimeException("课程等级为空");
            XueChengPlusException.cast("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
//            throw new RuntimeException("教育模式为空");
            XueChengPlusException.cast("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
//            throw new RuntimeException("适应人群为空");
            XueChengPlusException.cast("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
//            throw new RuntimeException("收费规则为空");
            XueChengPlusException.cast("收费规则为空");
        }*/

        //2.向课程基本信息表course_base写入信息
        CourseBase courseBaseNew = new CourseBase();
        //将传入的页面的参数放入对象中
        BeanUtils.copyProperties(dto, courseBaseNew);
        courseBaseNew.setCompanyId(companyId);
        courseBaseNew.setCreateDate(LocalDateTime.now());

        //审核状态默认为未提交
        courseBaseNew.setAuditStatus("202002");
        //发布状态为未发布
        courseBaseNew.setStatus("203001");

        int insert = courseBaseMapper.insert(courseBaseNew);
        if (insert <= 0) {
//            throw new RuntimeException("添加课程异常");
            XueChengPlusException.cast("添加课程异常");
        }

        //3.向课程营销表 course_marke写入如数据
        CourseMarket courseMarketNew = new CourseMarket();
        BeanUtils.copyProperties(dto, courseMarketNew);
        courseMarketNew.setId(courseBaseNew.getId());  //设置主键id

        //保存营销信息
        int i = saveCourseMarket(courseMarketNew);
        if (i <= 0) {
//            throw new RuntimeException("添加课程营销异常");
            XueChengPlusException.cast("添加课程营销异常");
        }

        //4.从数据库查出完整的信息
        return getCourseBaseInfo(courseBaseNew.getId());
    }


    //保存或更新营销信息
    private int saveCourseMarket(CourseMarket courseMarket) {
        //1.参数合法性校验
        if (StringUtils.isEmpty(courseMarket.getCharge())) {
//            throw new RuntimeException("收费规则为空");
            XueChengPlusException.cast("收费规则为空");
        }
        //如果收费规则 收费价格为空 抛异常
        if ("201001".equals(courseMarket.getCharge()) && (courseMarket.getPrice() == null ||
                courseMarket.getPrice() <= 0) || courseMarket.getOriginalPrice() <= 0) {
//            throw new RuntimeException("课程的价格不能为空并且大于0");
            XueChengPlusException.cast("课程的价格不能为空并且大于0");
        }

        //2.数据库查询营销信息
        CourseMarket courseMarketFormDB = courseMarketMapper.selectById(courseMarket.getId());
        if (courseMarketFormDB == null) {
            //新增
            return courseMarketMapper.insert(courseMarket);
        } else {
            //更新
            BeanUtils.copyProperties(courseMarket, courseMarketFormDB);
            courseMarketFormDB.setId(courseMarket.getId());
            return courseMarketMapper.updateById(courseMarketFormDB);
        }
    }

    //查询课程信息
    @Override
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        //1.查询课程基本信息表查询
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return null;
        }

        //2.查询营销表
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        if (courseMarket == null) {
            courseMarket = new CourseMarket();
        }

        //3.合并信息
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);

        //4.完善消数据
        CourseCategory mt = courseCategoryMapper.selectById(courseBaseInfoDto.getMt());
        CourseCategory st = courseCategoryMapper.selectById(courseBaseInfoDto.getSt());
        courseBaseInfoDto.setMtName(mt.getName());  //设置大分类名称
        courseBaseInfoDto.setStName(st.getName());  //设置小分类名称

        return courseBaseInfoDto;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //1.判断是否有修改权限
        Long id = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(id);
        //查询营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(id);
        if (courseBase == null || courseMarket == null) {
            XueChengPlusException.cast("课程不存在");
        }
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("本机构只能修改本机构课程");
        }


        //2.封装数据
        BeanUtils.copyProperties(editCourseDto, courseBase);
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        //3.修改时间
        courseBase.setCreateDate(LocalDateTime.now());

        //4.写入数据库
        int i1 = courseBaseMapper.updateById(courseBase);
        int i2 = this.saveCourseMarket(courseMarket);
        if (i1 <= 0 || i2 <= 0) {
            XueChengPlusException.cast("修改课程信息错误");
        }

        //5.查询课程信息
        return this.getCourseBaseInfo(id);
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        //1.查询审核状态是否为未提交
        //已提交，不能删除
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            return;
        }
        String status = courseBase.getStatus();
        if ("202002".equals(status)) {
            XueChengPlusException.cast("课程的审核状态为未提交方可删除");
        }

        //2.删除课程基本信息
        courseBaseMapper.deleteById(courseId);

        //3.删除课程营销信息
        courseMarketMapper.deleteById(courseId);

        //4.删除课程计划
        LambdaQueryWrapper<Teachplan> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Teachplan::getCourseId, courseId);
        teachplanMapper.delete(queryWrapper1);

        //5.删除教师信息
        LambdaQueryWrapper<CourseTeacher> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(CourseTeacher::getCourseId, courseId);
        courseTeacherMapper.delete(queryWrapper2);
    }

}
