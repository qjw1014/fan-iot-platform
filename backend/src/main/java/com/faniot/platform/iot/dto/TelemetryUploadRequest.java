package com.faniot.platform.iot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "盒子遥测上传请求")
public record TelemetryUploadRequest(
        @JsonAlias("gateway_id")
        @Schema(description = "盒子编号")
        @NotBlank
        String gatewayId,

        @JsonAlias("gateway_sn")
        @Schema(description = "盒子序列号")
        @NotBlank
        String gatewaySn,

        @JsonAlias("mqtt_username")
        @Schema(description = "MQTT用户名")
        @NotBlank
        String mqttUsername,

        @JsonAlias("mqtt_password")
        @Schema(description = "MQTT密码")
        @NotBlank
        String mqttPassword,

        @Schema(description = "遥测数据列表")
        @Valid
        List<TelemetryPointRequest> data
) {
}
