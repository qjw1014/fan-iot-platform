package com.faniot.platform.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "设备定位更新请求")
public record DeviceLocationRequest(
        @Schema(description = "安装位置") @Size(max = 255) String installLocation,
        @Schema(description = "纬度") BigDecimal latitude,
        @Schema(description = "经度") BigDecimal longitude,
        @Schema(description = "地址") @Size(max = 255) String address
) {
}
