package com.faniot.platform.d200.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record D200ConfigTaskRequest(
        @NotBlank String gatewayId,
        @NotBlank String gatewaySn,
        @NotBlank String taskType,
        @NotNull JsonNode configPayload
) {
}
