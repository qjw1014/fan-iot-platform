package com.faniot.platform.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "分页返回结构")
public record PageResponse<T>(
        @Schema(description = "数据列表") List<T> records,
        @Schema(description = "当前页，从1开始") int page,
        @Schema(description = "每页数量") int size,
        @Schema(description = "总记录数") long total,
        @Schema(description = "总页数") int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> pageData) {
        return new PageResponse<>(
                pageData.getContent(),
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
    }
}
