package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.SecurityUtil;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    private ContentServiceClient contentServiceClient;

    @Autowired
    private XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    private XcCourseTablesMapper xcCourseTablesMapper;

    @Transactional
    @Override
    public XcChooseCourseDto addChooseCourse(String userid, Long courseId) {

        //1.远程调用内容管理服务查询课程的收费规则
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if (coursepublish == null) {
            XueChengPlusException.cast("课程信息不存在");
        }

        String charge = coursepublish.getCharge();  //收费规则
        XcChooseCourse xcChooseCourse;
        if ("201000".equals(charge)) {
            //3.免费向选课记录表和选课记录表来写数据
            //免费课程
            xcChooseCourse = this.addFreeCoruse(userid, coursepublish);  //向选课记录表写
            XcCourseTables xcCourseTables = this.addCourseTabls(xcChooseCourse);  //向我的课程表写
        } else {
            //4.收费的向选课记录表写数据
            //收费课程
            xcChooseCourse = this.addChargeCoruse(userid, coursepublish);  //向选课记录表写
        }

        //5.判断学生的学习资格
        XcCourseTablesDto xcCourseTablesDto = this.getLearningStatus(userid, courseId);
        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse, xcChooseCourseDto);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());

        return xcChooseCourseDto;
    }

    //学习资格，[{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        //1.查询我的课程表
        XcCourseTables xcCourseTables = this.getXcCourseTables(userId, courseId);
        //无记录 没有选课 没有学习资格
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        if (xcCourseTables == null) {
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }

        //有记录 查询是否有效期
        LocalDateTime validtimeEnd = xcCourseTables.getValidtimeEnd();
        if (this.isOutTime(validtimeEnd)) {
            //过期不能学习
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }

        //没有过期 可以学习
        xcCourseTablesDto.setLearnStatus("702001");
        return xcCourseTablesDto;
    }

    @Transactional
    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        //1.查询选课表
        XcChooseCourse chooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if (chooseCourse == null) {
            log.debug("接收到购买课程的信息, 根据选课id从数据库找不到选课记录, 选课id:{}", chooseCourse);
            return false;
        }

        String status = chooseCourse.getStatus();  //选课状态
        if ("701002".equals(status)) {
            //更新选课记录的状态为支付成功
            chooseCourse.setStatus("701001");
            int updateChooseCourse = xcChooseCourseMapper.updateById(chooseCourse);
            if (updateChooseCourse <= 0) {
                log.debug("添加选课记录失败:{}", chooseCourse);
                XueChengPlusException.cast("添加选课记录失败");
            }

            //向课程表插入记录
            this.addCourseTabls(chooseCourse);
            return true;
        }

        return false;
    }

    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        //1.查询是否已经存在选课记录表中
        //选课成功直接返回
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId, userId);
        queryWrapper.eq(XcChooseCourse::getOrderType, "700001");  //免费课程
        queryWrapper.eq(XcChooseCourse::getStatus, "701001");  //选课成功

        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (!xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }

        //2.选课记录表没有数据，向选课记录表写数据
        XcChooseCourse xcChooseCourse = new XcChooseCourseDto();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");  //免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701001");  //选课成功
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());  //开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365L));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }
        return xcChooseCourse;
    }

    //添加到我的课程表
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse) {
        //1.查询课程表
        //有记录 不添加
        Long courseId = xcChooseCourse.getCourseId();
        String userId;
        try {
            userId = SecurityUtil.getUser().getId();
        } catch (Exception e) {
            userId = xcChooseCourse.getUserId();
        }
        XcCourseTables xcCourseTables = this.getXcCourseTables(userId, courseId);
        if (xcCourseTables != null) {
            return xcCourseTables;
        }

        //无记录 添加
        //2.选课成功才能添加
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)) {
            XueChengPlusException.cast("选课没有成功，无法添加到课程表");
        }

        //选课成功添加到课程表
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse, xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());  //记录选课表中的id
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());  //选课类型
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if (insert <= 0) {
            XueChengPlusException.cast("将选课课程添加到课程表失败");
        }

        return xcCourseTables;
    }

    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId, CoursePublish coursepublish) {
        //1.查询是否已经存在选课记录表中
        //选课成功直接返回
        Long courseId = coursepublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcChooseCourse::getUserId, userId);
        queryWrapper.eq(XcChooseCourse::getOrderType, "700002");  //收费课程
        queryWrapper.eq(XcChooseCourse::getStatus, "701002");  //选课状态未支付

        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (!xcChooseCourses.isEmpty()) {
            return xcChooseCourses.get(0);
        }

        //2.选课记录表没有数据，向选课记录表写数据
        XcChooseCourse xcChooseCourse = new XcChooseCourseDto();
        xcChooseCourse.setCourseId(courseId);
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");  //收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());
        xcChooseCourse.setValidDays(365);
        xcChooseCourse.setStatus("701002");  //选课成功 待支付
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());  //开始时间
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365L));

        int insert = xcChooseCourseMapper.insert(xcChooseCourse);
        if (insert <= 0) {
            XueChengPlusException.cast("添加选课记录失败");
        }

        return xcChooseCourse;
    }

    /**
     * 查询课程表
     *
     * @param userId   用户id
     * @param courseId 课程id
     * @return
     */
    private XcCourseTables getXcCourseTables(String userId, Long courseId) {
        LambdaQueryWrapper<XcCourseTables> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcCourseTables::getCourseId, courseId);
        queryWrapper.eq(XcCourseTables::getUserId, userId);
        return xcCourseTablesMapper.selectOne(queryWrapper);
    }

    private boolean isOutTime(LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        return endTime.isBefore(now);
    }

}
