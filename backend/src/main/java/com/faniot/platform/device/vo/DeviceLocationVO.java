package com.faniot.platform.device.vo;

import com.faniot.platform.device.domain.Device;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "设备定位信息")
public record DeviceLocationVO(
        String deviceId,
        String deviceName,
        String gatewayId,
        String customerId,
        String projectId,
        String installLocation,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String locationSource,
        OffsetDateTime lastLocationTime
) {
    public static DeviceLocationVO from(
            Device device,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            String locationSource,
            OffsetDateTime lastLocationTime
    ) {
        return new DeviceLocationVO(
                device.getDeviceId(),
                device.getDeviceName(),
                device.getGatewayId(),
                device.getCustomerId(),
                device.getProjectId(),
                device.getInstallLocation(),
                latitude,
                longitude,
                address,
                locationSource,
                lastLocationTime
        );
    }
}
