package com.xuecheng.base.exception;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
@Data
@AllArgsConstructor
public class DeleteErrorResponse implements Serializable {

    private String errCode;
    private String errMessage;


    public static DeleteErrorResponse createDeleteErrorResponse(DeleteErrorException exception) {
        return new DeleteErrorResponse(exception.getErrCode(), exception.getMessage());
    }
}
