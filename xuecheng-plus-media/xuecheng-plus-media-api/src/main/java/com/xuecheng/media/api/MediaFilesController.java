package com.xuecheng.media.api;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.config.UploadparamsDto;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 媒资文件管理接口
 * @date 2022/9/6 11:29
 */
@Api(value = "媒资文件管理接口", tags = "媒资文件管理接口")
@RestController
public class MediaFilesController {


    @Autowired
    MediaFileService mediaFileService;


    @ApiOperation("媒资列表查询接口")
    @PostMapping("/files")
    public PageResult<MediaFiles> list(PageParams pageParams, @RequestBody QueryMediaParamsDto queryMediaParamsDto) {
        Long companyId = 1232141425L;
        return mediaFileService.queryMediaFiels(companyId, pageParams, queryMediaParamsDto);

    }

    @ApiOperation("上传图片")
    @RequestMapping(value = "/upload/coursefile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadFileResultDto upload(@RequestParam("filedata")MultipartFile multipartFile) throws IOException {
        //准备上传参数数据
        UploadparamsDto uploadparamsDto = new UploadparamsDto();
        uploadparamsDto.setFilename(multipartFile.getOriginalFilename());  //原始文件名称
        uploadparamsDto.setFileSize(multipartFile.getSize());  //文件大小
        uploadparamsDto.setFileType("001001");


        //创建临时文件
        File tempFile = File.createTempFile("minio", "temp");
        multipartFile.transferTo(tempFile);

        //获取文件的路径
        String localFilePath = tempFile.getAbsolutePath();

        //调用service上传图片
        Long companyId = 12321441425L;
        UploadFileResultDto uploadFileResultDto = mediaFileService.uploadFile(uploadparamsDto, companyId, localFilePath);
        if (tempFile.exists()) {
            tempFile.delete();
        }
        return uploadFileResultDto;
    }

}
