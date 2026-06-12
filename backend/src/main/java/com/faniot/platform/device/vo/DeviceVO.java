package com.faniot.platform.device.vo;

import com.faniot.platform.device.domain.Device;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "设备信息")
public record DeviceVO(
        Long id,
        String deviceId,
        String gatewayId,
        String gatewaySn,
        String gatewayName,
        String customerId,
        String customerName,
        String projectId,
        String projectName,
        String deviceName,
        String deviceModel,
        String installLocation,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String locationSource,
        OffsetDateTime lastLocationTime,
        String status,
        OffsetDateTime lastSeenAt,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DeviceVO from(
            Device device,
            String gatewaySn,
            String gatewayName,
            String customerName,
            String projectName,
            BigDecimal latitude,
            BigDecimal longitude,
            String address,
            String locationSource,
            OffsetDateTime lastLocationTime
    ) {
        return new DeviceVO(
                device.getId(),
                device.getDeviceId(),
                device.getGatewayId(),
                gatewaySn,
                gatewayName,
                device.getCustomerId(),
                customerName,
                device.getProjectId(),
                projectName,
                device.getDeviceName(),
                device.getDeviceModel(),
                device.getInstallLocation(),
                latitude,
                longitude,
                address,
                locationSource,
                lastLocationTime,
                device.getStatus(),
                device.getLastSeenAt(),
                device.getRemark(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
