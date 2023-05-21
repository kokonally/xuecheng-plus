package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频处理任务类
 */
@Component
@Slf4j
public class VideoTaskJob {

    @Autowired
    private MediaFileProcessService mediaFileProcessService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;


    @Autowired
    private MediaFileService mediaFileService;

    @Autowired
    private MinioClient minioClient;


    @XxlJob("videoJobHandler")
    public void videoJobHandler() throws InterruptedException {
        int shardIndex = XxlJobHelper.getShardIndex();  //当前执行器序号
        int shardTotal = XxlJobHelper.getShardTotal();  //执行器总数量

        //获取CPU核心数
        int processors = Runtime.getRuntime().availableProcessors();
        log.debug("获取到CPU核心数:{}", processors);

        //1.获取任务列表
        List<MediaProcess> mediaProcessList
                = mediaFileProcessService.getMediaProcessList(shardTotal, shardIndex, processors);

        //创建线程池
        int taskNum = mediaProcessList.size();  //任务数量
        log.debug("获取到视频处理任务数量:{}", taskNum);
        if (taskNum == 0) {
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(taskNum);

        //使用计数器，等任务全部完成后再算完成此任务
        CountDownLatch countDownLatch = new CountDownLatch(taskNum);

        //2.开启任务
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    Long taskId = mediaProcess.getId();
                    boolean lock = mediaFileProcessService.startTask(taskId);
                    if (!lock) {
                        //拿不到任务跳过
                        log.debug("抢占任务失败,任务id:{}", taskId);
                        return;
                    }
                    log.debug("抢到任务:{}", mediaProcess.getId());
                    String filename = mediaProcess.getFilename();
                    String bucket = mediaProcess.getBucket();
                    String fileId = mediaProcess.getFileId();
                    String mp4_objectName = fileId.charAt(0) + "/" + fileId.charAt(1) + "/" + fileId + "/" + fileId + ".mp4";
                    if (".mp4".equals(mediaFileService.getExtension(filename))) {
                        //是mp4，无需转码
                        String url = "/" + bucket + "/" + mp4_objectName;  //新的URL
                        mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                        return;
                    }

                    //3.开始视频转码
                    //下载视频文件
                    String objectName = mediaProcess.getFilePath();
                    File downFile = mediaFileService.downloadFileFromMinio(bucket, objectName);
                    if (downFile == null || !downFile.exists()) {
                        log.error("从minio下载视频错误, 任务id:{}, bucket:{}, objectName:{}", taskId, bucket, objectName);
                        //上传失败消息到数据库
                        mediaFileProcessService
                                .saveProcessFinishStatus(taskId, "3", fileId, null, "从minio下载视频错误");
                        return;
                    }

                    String video_path = downFile.getAbsolutePath();
                    File file = null;
                    try {
                        file = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.error("创建临时文件异常, 错误信息:{}", e.getMessage());
                        //上传失败消息到数据库
                        mediaFileProcessService
                                .saveProcessFinishStatus(taskId, "3", fileId, null, "创建临时文件异常");
                        //上传失败消息到数据库
                        return;
                    }
                    String mp4_path = file.getAbsolutePath();
                    log.debug("视频正在转码, 正在进行的视频是:{}", filename);
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, filename, mp4_path);
                    String result = videoUtil.generateMp4();
                    if (!"success".equals(result)) {
                        //转换失败
                        log.debug("视频转码失败,bucket:{}, objectName:{},原因:{}", bucket, objectName, result);
                        mediaFileProcessService
                                .saveProcessFinishStatus(taskId, "3", fileId, null,
                                        "视频转码失败,原因:" + result);
                        //删除临时文件
                        if (downFile.exists()) {
                            downFile.delete();
                        } else if (file.exists()) {
                            file.delete();
                        }
                        return;
                    }
                    //转换成功
                    //4.上传文件到minio
                    //生成新的objectName
                    try {
                        boolean isUplocadSuccess = mediaFileService.upload2minio(mp4_path, mp4_objectName,
                                minioClient, bucket,
                                mediaFileService.getMimeType(".mp4"));
                        if (!isUplocadSuccess) {
                            //回传到minio失败
                            log.debug("视频转码回传到minio失败, taskId:{}, bucket:{}, objectName:{}",
                                    taskId, bucket, mp4_objectName);
                            mediaFileProcessService
                                    .saveProcessFinishStatus(taskId, "3", fileId, null,
                                            "视频转码回传到minio失败");
                        }
                    } finally {
                        //删除临时文件
                        if (downFile.exists()) {
                            downFile.delete();
                        } else if (file.exists()) {
                            file.delete();
                        }
                    }

                    //5.保存任务处理结果
                    String url = "/" + bucket + "/" + mp4_objectName;  //新的URL
                    mediaFileProcessService.saveProcessFinishStatus(taskId, "2", fileId, url, null);
                } finally {
                    countDownLatch.countDown();  //计数器 -1
                }
            });
        });

        //阻塞等待所有线程处理完任务,需要有一个最大限度的等待时间
        countDownLatch.await(30, TimeUnit.MINUTES);
    }
}
