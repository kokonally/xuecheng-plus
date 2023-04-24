package com.xuecheng.base.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class RestErrorResponse implements Serializable {
    private String errMessage;
}
