package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.media.service.impl.MediaFileServiceImpl;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class MinioTest {

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
}
