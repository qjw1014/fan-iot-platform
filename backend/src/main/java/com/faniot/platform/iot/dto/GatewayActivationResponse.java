package com.faniot.platform.iot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "盒子激活响应")
public record GatewayActivationResponse(
        @JsonProperty("gateway_id") String gatewayId,
        @JsonProperty("gateway_sn") String gatewaySn,
        @JsonProperty("activation_status") String activationStatus,
        @JsonProperty("mqtt_username") String mqttUsername,
        @JsonProperty("telemetry_topic") String telemetryTopic
) {
}
