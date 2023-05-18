package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.impl.MediaFileServiceImpl;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.List;
@SpringBootTest
public class MinioTest {
    @Autowired
    private MediaProcessMapper mediaProcessMapper;

    MinioClient minioClient = MinioClient
            .builder()
            .endpoint("http://192.168.44.130:9000")
            .credentials("minioadmin", "minioadmin")
            .build();
    @Test
    void test_upload() {
        ContentInfo contentInfo = ContentInfoUtil.findExtensionMatch(".mp4");
        String mimeType = contentInfo.getMimeType();

    }

    @Test
    void getExtensionTest() throws Exception {
        MediaFileServiceImpl mediaFileService = new MediaFileServiceImpl();

        Class<? extends MediaFileServiceImpl> aClass = mediaFileService.getClass();
        Method getExtension = aClass.getDeclaredMethod("getExtension", String.class);
        getExtension.setAccessible(true);
        String extension = (String) getExtension.invoke(mediaFileService, "testbucket/123/test/abc.mp4");
        System.out.println("extension = " + extension);
    }

    @Test
    void getMimeTypeTest() throws Exception {
        MediaFileServiceImpl mediaFileService = new MediaFileServiceImpl();

        Class<? extends MediaFileServiceImpl> aClass = mediaFileService.getClass();
        Method getExtension = aClass.getDeclaredMethod("getMimeType", String.class);
        getExtension.setAccessible(true);
        String mimeType = (String) getExtension.invoke(mediaFileService, ".mp4");
        System.out.println("mimeType = " + mimeType);
    }

    @Test
    void MediaProcessMapperTest() {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(1, 0, 2);
        System.out.println("mediaProcesses = " + mediaProcesses);
    }
}
