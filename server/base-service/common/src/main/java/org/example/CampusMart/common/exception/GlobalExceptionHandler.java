package org.example.CampusMart.common.exception;

import org.example.CampusMart.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result handle(Exception e){
        e.printStackTrace();
        return Result.fail();
    }

    @ExceptionHandler(CampusMartException.class)
    @ResponseBody
    public Result handle(CampusMartException e){
        e.printStackTrace();
        return Result.fail(e.getCode(),e.getMessage());
    }
}
