package org.example.CampusMart.common.exception;


import lombok.Data;
import org.example.CampusMart.common.result.ResultCodeEnum;

@Data
public class CampusMartException extends RuntimeException{

    private Integer code;

    public CampusMartException(Integer code, String message){
        super(message);
        this.code = code;
    }

    public CampusMartException(ResultCodeEnum resultCodeEnum){
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }
}
