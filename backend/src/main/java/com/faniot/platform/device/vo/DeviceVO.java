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
        String status,
        OffsetDateTime lastSeenAt,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DeviceVO from(Device device, String gatewayName, String customerName, String projectName) {
        return new DeviceVO(
                device.getId(),
                device.getDeviceId(),
                device.getGatewayId(),
                gatewayName,
                device.getCustomerId(),
                customerName,
                device.getProjectId(),
                projectName,
                device.getDeviceName(),
                device.getDeviceModel(),
                device.getInstallLocation(),
                device.getLatitude(),
                device.getLongitude(),
                device.getAddress(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getRemark(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
