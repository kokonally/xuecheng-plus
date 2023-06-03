package com.xuecheng.content.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.*;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.CourseTeacherService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    private MediaServiceClient mediaServiceClient;

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

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @Autowired
    private MqMessageService mqMessageService;


    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {
        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        //1.查询基本信息和营销信息
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        coursePreviewDto.setCourseBase(courseBaseInfo);

        //2.查询课程计划信息
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDto.setTeachplans(teachplanTree);

        //3.查询课程教师信息
        List<CourseTeacher> teachers = courseTeacherService.list(courseId);
        coursePreviewDto.setTeachers(teachers);

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

    @Transactional
    @Override
    public void coursepublish(Long companyId, Long courseId) {
        //1.将课程从预发布表copy到发布表中
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre == null) {
            XueChengPlusException.cast("课程无审核记录，不允许发布");
        }

        //没有审核通过不允许发布
        String status = coursePublishPre.getStatus();
        if (!"202004".equals(status)) {
            XueChengPlusException.cast("课程审核不通过，不允许发布");
        }
        //不能发布其他机构的课程
        Long companyIdFromCourse = coursePublishPre.getCompanyId();
        if (!companyIdFromCourse.equals(companyId)) {
            XueChengPlusException.cast("你无权操作其他机构的课程");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre, coursePublish);
        LocalDateTime now = LocalDateTime.now();
        coursePublish.setCreateDate(now);
        coursePublish.setOfflineDate(now);

        CoursePublish coursePublishFromDB = coursePublishMapper.selectById(courseId);
        if (coursePublishFromDB == null) {
            //数据库中无记录 插入
            coursePublishMapper.insert(coursePublish);
        } else {
            //有记录 修改
            coursePublishMapper.updateById(coursePublish);
        }
        //修改课程基本信息表
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203001");
        courseBaseMapper.updateById(courseBase);

        //2.写入消息任务表中
        this.saveCoursePublishMessage(courseId);

        //3.删除预发布表
        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {

        Configuration configuration = new Configuration(Configuration.getVersion());
        File file = null;
        try {
            //拿到classpath路径
            String classpath = this.getClass().getResource("/").getPath();


            String path = classpath + "templates";

            //指定模板目录
            configuration.setDirectoryForTemplateLoading(new File(path));

            //指定编码
            configuration.setDefaultEncoding("utf-8");

            //得到模板
            Template template = configuration.getTemplate("course_template.ftl");
            //Template template 模板, Object model 数据模型
            CoursePreviewDto coursePreviewDto = this.getCoursePreviewInfo(courseId);
            Map<String, CoursePreviewDto> model = Collections.singletonMap("model", coursePreviewDto);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);

            //将HTML写入文件
            file = File.createTempFile(courseId.toString(), ".html");

            try (InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
                 OutputStream outputStream = new FileOutputStream(file);) {
                IOUtils.copy(inputStream, outputStream);
            }

        } catch (Exception e) {
            log.error("页面静态化出现错误, 课程id:{}, 错误信息:{}", courseId, e);
        }

        return file;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        //1.调用远程上传接口
        try {
            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
            String objectName = "course/" + courseId + ".html";
            String upload = mediaServiceClient.upload(multipartFile, objectName);
            if (upload == null) {
                log.debug("远程调用走降级逻辑得到上传的结果为null, 课程id：{}", courseId);
                XueChengPlusException.cast("上传静态文件过程中存在异常");
            }
        } finally {
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        return coursePublishMapper.selectById(courseId);
    }

    /**
     * 加入到发布同步表
     *
     * @param courseId 课程id
     */
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage course_publish = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if (course_publish == null) {
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
    }
}
