package com.xuecheng.content.service.jobhandler;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 课程发布任务类
 */
@Component
@Slf4j
public class CoursePublishTask extends MessageProcessAbstract {

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
        Long courseId = Long.valueOf(mqMessage.getStageState1());

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
        int stageOne = mqMessageService.getStageOne(courseId);
        if (stageOne > 0) {
            //已经完成
            log.debug("课程静态化已经完成，无需处理....");
            return;
        }

        //2.开始处理任务

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
        int stageTwo = mqMessageService.getStageTwo(courseId);
        if (stageTwo > 0) {
            //已经完成
            log.debug("课程保存索引已经完成，无需处理....");
            return;
        }

        //2.开始处理任务

        //3.写任务状态为完成
        mqMessageService.completedStageTwo(courseId);
    }
}
