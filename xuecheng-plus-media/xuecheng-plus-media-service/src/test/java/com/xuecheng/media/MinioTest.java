package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

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
}
