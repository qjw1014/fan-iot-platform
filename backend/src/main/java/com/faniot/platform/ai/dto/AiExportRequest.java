package com.faniot.platform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Schema(description = "AI数据导出请求")
public record AiExportRequest(
        @Schema(description = "设备编号") @NotBlank String deviceId,
        @Schema(description = "导出格式：json/csv") String format,
        @Schema(description = "开始时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
        @Schema(description = "结束时间") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
        @Schema(description = "最大记录数") Integer limit
) {
    public String safeFormat() {
        return format == null || format.isBlank() ? "json" : format.toLowerCase();
    }

    public int safeLimit() {
        return limit == null ? 1000 : Math.min(Math.max(limit, 1), 5000);
    }
}
