package com.faniot.platform.monitor.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RealtimeMetricVO(
        String gatewayId,
        String deviceId,
        String deviceName,
        String status,
        BigDecimal rpm,
        BigDecimal current,
        BigDecimal voltage,
        BigDecimal power,
        BigDecimal frequency,
        BigDecimal pressure,
        BigDecimal airflow,
        BigDecimal motorTemperature,
        BigDecimal bearingTemperature,
        BigDecimal vibration,
        String alarmCode,
        OffsetDateTime timestamp,
        OffsetDateTime receivedAt
) {
}
