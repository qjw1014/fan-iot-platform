package com.faniot.platform.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "遥测上传响应")
public record TelemetryUploadResponse(
        @JsonProperty("gateway_id") String gatewayId,
        @JsonProperty("saved_count") int savedCount
) {
}
