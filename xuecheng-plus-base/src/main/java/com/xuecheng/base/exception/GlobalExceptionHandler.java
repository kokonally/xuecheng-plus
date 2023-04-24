package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  //状态码500
    public RestErrorResponse customException(XueChengPlusException e) {
        //记录异常
        log.error("系统异常{}", e.getErrMessage(), e);

        //解析出异常信息
        String errMessage = e.getErrMessage();
        return new RestErrorResponse(errMessage);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  //状态码500
    public RestErrorResponse customException(Exception e) {
        //记录异常
        log.error("系统异常{}", e.getMessage(), e);

        //解析出异常信息
        return new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
    }

}
