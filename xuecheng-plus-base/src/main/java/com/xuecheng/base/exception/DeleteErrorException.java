package com.xuecheng.base.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DeleteErrorException extends RuntimeException {

    private String errCode;

    public DeleteErrorException(String message, String errCode) {
        super(message);
        this.errCode = errCode;
    }
}
