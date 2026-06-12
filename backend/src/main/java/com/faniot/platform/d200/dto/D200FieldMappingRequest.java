package com.faniot.platform.d200.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record D200FieldMappingRequest(
        @NotBlank String gatewayId,
        @NotBlank String deviceId,
        @NotBlank String sourceKey,
        @NotBlank String targetField,
        BigDecimal scaleFactor,
        BigDecimal offsetValue,
        String unit,
        Boolean enabled
) {
}
