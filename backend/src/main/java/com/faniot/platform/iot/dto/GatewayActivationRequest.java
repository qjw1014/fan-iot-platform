package com.faniot.platform.iot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "盒子激活请求")
public record GatewayActivationRequest(
        @JsonAlias("gateway_id")
        @Schema(description = "盒子编号")
        @NotBlank
        @Size(max = 64)
        String gatewayId,

        @JsonAlias("gateway_sn")
        @Schema(description = "盒子序列号")
        @NotBlank
        @Size(max = 96)
        String gatewaySn,

        @JsonAlias("mqtt_username")
        @Schema(description = "MQTT用户名")
        @NotBlank
        @Size(max = 120)
        String mqttUsername,

        @JsonAlias("mqtt_password")
        @Schema(description = "MQTT密码")
        @NotBlank
        @Size(max = 120)
        String mqttPassword
) {
}
