package com.petcare.common.web;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class, BindException.class, MethodArgumentNotValidException.class})
    public ApiResponse<Void> handleBadRequest(Exception e) {
        return ApiResponse.fail("请求参数错误: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleGeneric(Exception e) {
        return ApiResponse.fail("服务器异常: " + e.getMessage());
    }
}


