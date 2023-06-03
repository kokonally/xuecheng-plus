package com.xuecheng.content.service.jobhandler;

import cn.hutool.core.util.BooleanUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 课程发布任务类
 */
@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    private CoursePublishService coursePublishService;

    @Autowired
    private SearchServiceClient searchServiceClient;

    @Autowired
    private CoursePublishMapper coursePublishMapper;

    @XxlJob("CoursePublishJobHandler")
    public void coursePublicJobHandle() {
        //分片数量
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //执行
        this.process(shardIndex, shardTotal, "course_publish", 30, 60);
    }


    @Override
    public boolean execute(MqMessage mqMessage) {
        //从mqMessage拿到课程id
        Long courseId = Long.valueOf(mqMessage.getBusinessKey1());

        //1.课程静态化写入minio
        this.generateCourseHtml(mqMessage, courseId);

        //2.向elasticsearch写入索引库
        this.saveCourseIndex(mqMessage, courseId);

        //3.向redis写缓存

        return true;  //true 表示任务完成
    }

    /**
     * 生成课程静态页面上传到minio
     *
     * @param mqMessage 课程消息
     * @param courseId  课程id
     */
    public void generateCourseHtml(MqMessage mqMessage, long courseId) {
        Long taskId = mqMessage.getId();  //任务id
        MqMessageService mqMessageService = this.getMqMessageService();
        //1.任务幂等性处理
        //查询数据库

        int stageOne = mqMessageService.getStageOne(String.valueOf(courseId), null,null);
        if (stageOne > 0) {
            //已经完成
            log.debug("课程静态化已经完成，无需处理....");
            return;
        }

        //2.开始处理任务
        //生成html静态化页面
        File courseHtml = coursePublishService.generateCourseHtml(courseId);

        //上传到minio
        if (courseHtml != null) {
            coursePublishService.uploadCourseHtml(courseId, courseHtml);
        }

        //3.写任务状态为完成
        mqMessageService.completedStageOne(courseId);
    }

    /**
     * 保存课程索引
     *
     * @param mqMessage 任务消息
     * @param courseId  课程id
     */
    private void saveCourseIndex(MqMessage mqMessage, long courseId) {
        Long taskId = mqMessage.getId();  //任务id
        MqMessageService mqMessageService = this.getMqMessageService();
        //1.任务幂等性处理
        //查询数据库
        int stageTwo = mqMessageService.getStageTwo(String.valueOf(courseId), null, null);
        if (stageTwo > 0) {
            //已经完成
            log.debug("课程保存索引已经完成，无需处理....");
            return;
        }

        //2.开始处理任务
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);//查询课程信息
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish, courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if (BooleanUtil.isFalse(add)) {
            XueChengPlusException.cast("远程调用搜索方法添加课程索引失败");
        }

        //3.写任务状态为完成
        mqMessageService.completedStageTwo(courseId);
    }
}
