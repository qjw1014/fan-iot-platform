package com.faniot.platform.ai.vo;

import com.faniot.platform.device.vo.DeviceVO;

import java.math.BigDecimal;

public record AiDeviceMetaVO(
        String deviceId,
        String deviceName,
        String gatewayId,
        String gatewaySn,
        String customerId,
        String customerName,
        String projectId,
        String projectName,
        BigDecimal latitude,
        BigDecimal longitude,
        String address
) {
    public static AiDeviceMetaVO from(DeviceVO device) {
        return new AiDeviceMetaVO(
                device.deviceId(),
                device.deviceName(),
                device.gatewayId(),
                device.gatewaySn(),
                device.customerId(),
                device.customerName(),
                device.projectId(),
                device.projectName(),
                device.latitude(),
                device.longitude(),
                device.address()
        );
    }
}
