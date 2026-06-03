package com.faniot.platform.monitor.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TelemetryHistoryPointVO(
        OffsetDateTime timestamp,
        BigDecimal rpm,
        BigDecimal current,
        BigDecimal voltage,
        BigDecimal power,
        BigDecimal motorTemperature,
        BigDecimal bearingTemperature,
        BigDecimal vibration
) {
}
