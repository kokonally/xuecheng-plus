package com.xuecheng.media.service.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.config.UploadparamsDto;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author Mr.M
 * @version 1.0
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    private MediaFilesMapper mediaFilesMapper;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.files}")
    private String mediafiles;  //存储普通文件
    @Value("${minio.bucket.videofiles}")
    private String video;  //存储视频


    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(UploadparamsDto uploadparamsDto, Long companyId, String localFilePath) {
        //1.将文件上传到minio
        String extension = this.getExtension(uploadparamsDto.getFilename());  //获取到文件扩展名
        String mimeType = this.getMimeType(extension);  //获取文件的mimeType类型
        //生成minio上的路径 以年月日下存储
        String dateTimePath = this.getDateTimePath();
        //获取objectFileName
        String md5 = DigestUtil.md5Hex(new File(localFilePath));
        String objectFileName = dateTimePath + md5 + extension;

        //判断在数据库中文件是否存在
        MediaFiles mediaFilesFromDb = mediaFilesMapper.selectById(md5);
        if (mediaFilesFromDb != null) {
            UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
            BeanUtils.copyProperties(mediaFilesFromDb, uploadFileResultDto);
            return uploadFileResultDto;
        }

        boolean isSuccess = this.upload2minio(localFilePath, objectFileName, minioClient, mediafiles, mimeType);
        if (!isSuccess) {
            XueChengPlusException.cast("上传文件失败");
        }
        //2.将文件信息保存到数据库
        //查询文件是否存在
        MediaFileServiceImpl proxy = (MediaFileServiceImpl) AopContext.currentProxy();
        MediaFiles mediaFiles = proxy.mediaFiles2Db(companyId, md5, uploadparamsDto, mediafiles, objectFileName);

        //3.准备返回的对象
        UploadFileResultDto uploadFileResultDto = new UploadFileResultDto();
        if (mediaFiles == null) {
            XueChengPlusException.cast("文件上传保存信息失败");
        }
        BeanUtils.copyProperties(mediaFiles, uploadFileResultDto);

        return uploadFileResultDto;
    }

    /**
     * 生成存入数据库的mediafile
     *
     * @param companyId 机构id
     * @param fileMD5   文件的md5
     * @param bucket    桶
     * @return mediaFiles
     */
    @Transactional
    public MediaFiles mediaFiles2Db(Long companyId, String fileMD5,
                                     UploadparamsDto uploadparamsDto,
                                     String bucket, String objectFileName) {
        //文件不存在 写入新的
        MediaFiles mediaFiles = new MediaFiles();
        BeanUtils.copyProperties(uploadparamsDto, mediaFiles);
        //设置文件id
        mediaFiles.setId(fileMD5);
        //机构id
        mediaFiles.setCompanyId(companyId);
        //桶
        mediaFiles.setBucket(bucket);
        //存储路径
        mediaFiles.setFilename(objectFileName);
        //file_id
        mediaFiles.setFileId(fileMD5);
        //url
        mediaFiles.setUrl("/" + bucket + "/" + objectFileName);
        //上传时间
        mediaFiles.setCreateDate(LocalDateTime.now());
        //状态
        mediaFiles.setStatus("1");
        //审核状态
        mediaFiles.setAuditStatus("002003");

        //写入数据库
        int insert = mediaFilesMapper.insert(mediaFiles);
        if (insert <= 0) {
            log.debug("向数据库保存文件信息失败，bucket:{}, objectName:{}", bucket, objectFileName);
            return null;
        }
        return mediaFiles;
    }


    /**
     * 获取当前的时间路径  2023/5/10/
     *
     * @return 2023/5/10/
     */
    private String getDateTimePath() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return dateStr.replace("-", "/") + "/";
    }

    /**
     * 根据扩展名获取mimeType
     *
     * @return mimeType
     */
    private String getMimeType(String extensionName) {
        if (extensionName == null) {
            return null;
        }
        ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(extensionName);
        if (contentInfo != null) {
            return contentInfo.getMimeType();
        }
        return null;
    }

    /**
     * 上传文件到minio
     *
     * @param localFilePath  源文件路径
     * @param objectFilePath 在minio文件的路径
     * @param minioClient    minio客户端
     * @param bucket         指定上传到的桶
     * @param mimeType       文件类型
     * @return true上传成功 false上传失败
     */
    private boolean upload2minio(String localFilePath, String objectFilePath,
                                 MinioClient minioClient, String bucket, String mimeType) {
        try {
            UploadObjectArgs args = UploadObjectArgs.builder()
                    .bucket(bucket)
                    .filename(localFilePath)
                    .object(objectFilePath)
                    .contentType(mimeType)
                    .build();

            minioClient.uploadObject(args);
            log.debug("上传文件到minio成功, bucket:{}, objectName:{}", bucket, objectFilePath);
            return true;
        } catch (Exception e) {
            log.error("上传文件出错, bucket:{}, objectName:{}, 错误信息:{}", bucket, objectFilePath, e);
        }
        return false;
    }

    /**
     * 获取文件扩展名
     *
     * @param orginName 原始文件名称（或者路径）
     * @return 扩展名
     */
    private String getExtension(String orginName) {
        return orginName.substring(orginName.lastIndexOf("."));
    }
}
