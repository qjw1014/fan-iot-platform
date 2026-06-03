package com.faniot.platform.iot.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.iot.dto.GatewayActivationRequest;
import com.faniot.platform.iot.dto.GatewayActivationResponse;
import com.faniot.platform.iot.dto.TelemetryUploadRequest;
import com.faniot.platform.iot.dto.TelemetryUploadResponse;
import com.faniot.platform.iot.service.IotIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IoT数据接入", description = "盒子激活、HTTP遥测上传")
@RestController
@RequestMapping("/api/iot")
public class IotController {

    private final IotIngestionService iotIngestionService;

    public IotController(IotIngestionService iotIngestionService) {
        this.iotIngestionService = iotIngestionService;
    }

    @Operation(summary = "盒子激活")
    @PostMapping("/gateway/activate")
    public ApiResponse<GatewayActivationResponse> activate(@Valid @RequestBody GatewayActivationRequest request) {
        return ApiResponse.ok(iotIngestionService.activate(request));
    }

    @Operation(summary = "HTTP遥测上传")
    @PostMapping("/telemetry")
    public ApiResponse<TelemetryUploadResponse> telemetry(@Valid @RequestBody TelemetryUploadRequest request) {
        return ApiResponse.ok(iotIngestionService.ingestTelemetry(request, null));
    }
}
