package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.config.UploadparamsDto;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import io.minio.MinioClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理业务类
 * @date 2022/9/10 8:55
 */
public interface MediaFileService {

    /**
     * 根据媒资id查询文件信息
     * @param mediaId 媒资id
     * @return
     */
    MediaFiles getFileById(String mediaId);

    /**
     * @param pageParams          分页参数
     * @param queryMediaParamsDto 查询条件
     * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
     * @description 媒资文件查询方法
     * @author Mr.M
     * @date 2022/9/10 8:57
     */
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

    /**
     * 上传文件
     *
     * @param uploadparamsDto 上传参数
     * @param companyId       机构id
     * @param localFilePath   文件的本地路径
     * @param objectName   文件的需要上传的路径 如果传入了objectName了就按照objectName来存储
     * @return UploadFileResultDto
     */
    UploadFileResultDto uploadFile(UploadparamsDto uploadparamsDto, Long companyId, String localFilePath, String objectName);

    /**
     * 上传文件前查询文件
     * @param fileMd5 文件的MD5
     * @return RestResponse<Boolean>
     */
    RestResponse<Boolean> checkFile(String fileMd5);

    /**
     * 上传分块前检测分块
     * @param fileMd5 文件的md5值
     * @param chunk 第？个分块
     * @return RestResponse<Boolean>
     */
    RestResponse<Boolean> checkchunk(String fileMd5, int chunk);

    /**
     * 上传分块文件
     * @param fileMd5 完整文件md5值
     * @param chunk 上传的块文件
     * @param localChunkFilePath 需上传的文件路径
     * @return RestResponse
     */
    RestResponse uploadchunk(String fileMd5, int chunk, String localChunkFilePath);

    /**
     * 合并分块文件
     * @param companyId 机构id
     * @param fileMd5 文件的md5值
     * @param chunkTotal 总的文件块数量
     * @param uploadparamsDto 入库信息
     * @return
     */
    RestResponse mergechunks(Long companyId, String fileMd5, int chunkTotal, UploadparamsDto uploadparamsDto);

    /**
     * 从minio下载文件
     * @param bucket 桶
     * @param objectName 文件名称
     * @return file
     */
    File downloadFileFromMinio(String bucket, String objectName);

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
    public boolean upload2minio(String localFilePath, String objectFilePath,
                                MinioClient minioClient, String bucket, String mimeType);

    /**
     * 获取mimeType
     * @param extensionName 后缀名称
     * @return mimeType
     */
    public String getMimeType(String extensionName);

    public String getExtension(String orginName);

}
