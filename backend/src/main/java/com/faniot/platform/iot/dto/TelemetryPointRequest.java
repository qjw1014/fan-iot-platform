package com.faniot.platform.iot.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "单设备遥测数据")
public record TelemetryPointRequest(
        @JsonAlias("device_id")
        @Schema(description = "设备编号")
        @NotBlank
        String deviceId,

        @Schema(description = "采集时间")
        OffsetDateTime timestamp,

        BigDecimal rpm,
        BigDecimal current,
        BigDecimal voltage,
        BigDecimal power,
        BigDecimal frequency,
        BigDecimal pressure,
        BigDecimal airflow,

        @JsonAlias({"motor_temperature", "temperature"})
        BigDecimal motorTemperature,

        @JsonAlias("bearing_temperature")
        BigDecimal bearingTemperature,

        BigDecimal vibration,
        String status,

        @JsonAlias("alarm_code")
        String alarmCode
) {
}
