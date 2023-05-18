package com.xuecheng.media.service.impl;

import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MediaProcessHistoryMapper mediaProcessHistoryMapper;

    @Override
    public List<MediaProcess> getMediaProcessList(int shardTotal, int shardIndex, int count) {
        return mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
    }

    @Override
    public boolean startTask(long id) {
        return mediaProcessMapper.startTask(id) == 1;
    }

    @Override
    @Transactional
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        //1.查询任务表
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if (mediaProcess == null) {
            return;
        }
        //2.更新表
        if ("3".equals(status)) {
            //任务执行失败
            mediaProcess.setStatus("3");
            mediaProcess.setErrormsg(errorMsg);
            mediaProcess.setFailCount(mediaProcess.getFailCount() + 1);
            mediaProcessMapper.updateById(mediaProcess);
            return;
        }

        //任务执行成功
        //更新文件表的url 向任务历史表插入数据 删除任务表
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        mediaFiles.setUrl(url);
        mediaFiles.setFilename(mediaFiles.getFilename()
                .substring(0, mediaFiles.getFilename().lastIndexOf(".")) + ".mp4");
        mediaFilesMapper.updateById(mediaFiles);

        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);
        mediaProcessMapper.updateById(mediaProcess);

        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess, mediaProcessHistory);
        mediaProcessHistory.setFilename(mediaProcessHistory.getFilename()
                .substring(0, mediaProcessHistory.getFilename().lastIndexOf(".")) + ".mp4");

        mediaProcessHistoryMapper.insert(mediaProcessHistory);
        mediaProcessMapper.deleteById(taskId);

    }
}
