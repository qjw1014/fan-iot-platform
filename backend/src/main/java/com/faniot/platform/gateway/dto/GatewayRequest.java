package com.faniot.platform.gateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "盒子保存请求")
public record GatewayRequest(
        @Schema(description = "盒子编号") @NotBlank @Size(max = 64) String gatewayId,
        @Schema(description = "盒子SN") @NotBlank @Size(max = 96) String gatewaySn,
        @Schema(description = "盒子名称") @NotBlank @Size(max = 120) String gatewayName,
        @Schema(description = "盒子型号") @Size(max = 120) String gatewayModel,
        @Schema(description = "IMEI") @Size(max = 32) String imei,
        @Schema(description = "ICCID") @Size(max = 32) String iccid,
        @Schema(description = "SIM卡号") @Size(max = 32) String simCardNo,
        @Schema(description = "客户编号") @Size(max = 64) String customerId,
        @Schema(description = "项目编号") @Size(max = 64) String projectId,
        @Schema(description = "激活状态") @Pattern(regexp = "inactive|active|disabled") String activationStatus,
        @Schema(description = "在线状态") @Pattern(regexp = "online|offline") String onlineStatus,
        @Schema(description = "MQTT Client ID") @Size(max = 96) String mqttClientId,
        @Schema(description = "MQTT用户名") @Size(max = 120) String mqttUsername,
        @Schema(description = "MQTT密码，仅保存哈希") @Size(max = 120) String mqttPassword,
        @Schema(description = "D200上行Topic") @Size(max = 128) String publishTopic,
        @Schema(description = "D200下行Topic") @Size(max = 128) String subscribeTopic,
        @Schema(description = "MQTT版本") @Pattern(regexp = "3\\.1|3\\.1\\.1") String mqttVersion,
        @Schema(description = "QoS") Integer qos,
        @Schema(description = "Keepalive秒") Integer keepalive,
        @Schema(description = "是否启用TLS，D200固定为false") Boolean tlsEnabled,
        @Schema(description = "是否支持远程配置") Boolean remoteConfigSupported,
        @Schema(description = "固件版本") @Size(max = 64) String firmwareVersion,
        @Schema(description = "纬度") BigDecimal latitude,
        @Schema(description = "经度") BigDecimal longitude,
        @Schema(description = "地址") @Size(max = 255) String address,
        @Schema(description = "省") @Size(max = 80) String province,
        @Schema(description = "市") @Size(max = 80) String city,
        @Schema(description = "区县") @Size(max = 80) String district,
        @Schema(description = "定位来源") @Pattern(regexp = "manual|lbs") String locationSource,
        @Schema(description = "备注") @Size(max = 500) String remark
) {
}
