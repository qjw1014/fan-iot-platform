package com.faniot.platform.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "统一返回结构")
public record ApiResponse<T>(
        @Schema(description = "业务状态码") int code,
        @Schema(description = "提示信息") String message,
        @Schema(description = "响应数据") T data,
        @Schema(description = "响应时间") OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "成功", data, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "成功", null, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, OffsetDateTime.now());
    }
}
