package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaServiceClient降级熔断策略工厂
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactroy implements FallbackFactory<MediaServiceClient> {
    //拿到了当时熔断的异常信息
    @Override
    public MediaServiceClient create(Throwable throwable) {
        return new MediaServiceClient() {
            //发生熔断，上级服务就会调用此降级的逻辑
            @Override
            public String upload(MultipartFile multipartFile, String objectName) {
                log.debug("远程调用上传文件接口发生了熔断:{}", throwable.toString(), throwable);
                return null;
            }
        };
    }
}
