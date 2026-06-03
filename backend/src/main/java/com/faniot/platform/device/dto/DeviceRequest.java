package com.faniot.platform.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "设备保存请求")
public record DeviceRequest(
        @Schema(description = "设备编号") @NotBlank @Size(max = 64) String deviceId,
        @Schema(description = "盒子编号") @NotBlank @Size(max = 64) String gatewayId,
        @Schema(description = "客户编号") @Size(max = 64) String customerId,
        @Schema(description = "项目编号") @Size(max = 64) String projectId,
        @Schema(description = "设备名称") @NotBlank @Size(max = 120) String deviceName,
        @Schema(description = "设备型号") @Size(max = 120) String deviceModel,
        @Schema(description = "安装位置") @Size(max = 255) String installLocation,
        @Schema(description = "纬度") BigDecimal latitude,
        @Schema(description = "经度") BigDecimal longitude,
        @Schema(description = "地址") @Size(max = 255) String address,
        @Schema(description = "设备状态") @Pattern(regexp = "online|offline|alarm|maintenance|disabled") String status,
        @Schema(description = "备注") @Size(max = 500) String remark
) {
}
