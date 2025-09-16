package com.petcare.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一API响应格式")
public class ApiResponse<T> {
    
    @Schema(description = "响应码，0表示成功", example = "0")
    private int code;
    
    @Schema(description = "响应消息", example = "ok")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(-1, msg, null);
    }
}


