package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<String> errors = new ArrayList<>();
        bindingResult.getFieldErrors().forEach(item -> {
            errors.add(item.getDefaultMessage());
        });

        //将list里的错误信息拼接起来
        String errMessage = StringUtils.join(errors, ",");

        //记录异常
        log.error("系统异常{}", e.getMessage(), errMessage);

        return new RestErrorResponse(errMessage);
    }

    @ExceptionHandler(DeleteErrorException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public DeleteErrorResponse DeleteErrorException(DeleteErrorException exception) {
        //1.记录异常
        log.error("删除操作异常 {}", exception.getMessage(), exception);

        //2.返回异常结果
        return DeleteErrorResponse.createDeleteErrorResponse(exception);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse DuplicateKeyException(DuplicateKeyException exception) {
        String message = exception.getMessage();
        if (message.contains("course_teacher.courseid_teacherId_unique")) {
            return new RestErrorResponse("同一个课程中不允许存在同名教师");
        }

        return customException(exception);
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
