package com.xuecheng.media.service;

import com.xuecheng.media.model.po.MediaProcess;

import java.util.List;

/**
 *
 */
public interface MediaFileProcessService {

    /**
     * 查询所在执行器需要执行的任务
     * @param shardTotal 执行器的总数量
     * @param shardIndex 当前执行器的索引
     * @param count 查询的数量
     * @return
     */
    public abstract List<MediaProcess> getMediaProcessList(int shardTotal, int shardIndex, int count);

    /**
     * 开始任务
     * @param id 需要执行的任务id
     * @return true可以执行 false无执行权
     */
    boolean startTask(long id);

    /**
     * 保存任务结果
     * @param taskId 任务id
     * @param status 任务状态
     * @param fileId 文件id
     * @param url URL
     * @param errorMsg 错误信息
     */
    void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg);


}
