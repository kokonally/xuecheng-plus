package com.xuecheng.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class XueChengPlusException extends RuntimeException {

    private String errMessage;

    public XueChengPlusException() {
    }

    public XueChengPlusException(String message) {
        super(message);
        this.errMessage = message;
    }

    public static void cast(String mmessage) {
        throw new XueChengPlusException(mmessage);
    }
    public static void cast(CommonError commonError) {
        throw new XueChengPlusException(commonError.getErrMessage());
    }
}
