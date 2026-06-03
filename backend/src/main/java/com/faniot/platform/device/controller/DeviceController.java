package com.faniot.platform.device.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.device.dto.DeviceLocationRequest;
import com.faniot.platform.device.dto.DeviceRequest;
import com.faniot.platform.device.service.DeviceService;
import com.faniot.platform.device.vo.DeviceLocationVO;
import com.faniot.platform.device.vo.DeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "设备管理", description = "设备新增、编辑、删除和分页查询")
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "分页查询设备")
    @GetMapping
    public ApiResponse<PageResponse<DeviceVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String gatewayId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(deviceService.page(keyword, customerId, projectId, gatewayId, status, page, size));
    }

    @Operation(summary = "设备详情")
    @GetMapping("/{id}")
    public ApiResponse<DeviceVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.detail(id));
    }

    @Operation(summary = "设备定位详情")
    @GetMapping("/{deviceId}/location")
    public ApiResponse<DeviceLocationVO> location(@PathVariable String deviceId) {
        return ApiResponse.ok(deviceService.location(deviceId));
    }

    @Operation(summary = "更新设备定位")
    @PutMapping("/{deviceId}/location")
    public ApiResponse<DeviceLocationVO> updateLocation(
            @PathVariable String deviceId,
            @Valid @RequestBody DeviceLocationRequest request
    ) {
        return ApiResponse.ok(deviceService.updateLocation(deviceId, request));
    }

    @Operation(summary = "新增设备")
    @PostMapping
    public ApiResponse<DeviceVO> create(@Valid @RequestBody DeviceRequest request) {
        return ApiResponse.ok(deviceService.create(request));
    }

    @Operation(summary = "编辑设备")
    @PutMapping("/{id}")
    public ApiResponse<DeviceVO> update(@PathVariable Long id, @Valid @RequestBody DeviceRequest request) {
        return ApiResponse.ok(deviceService.update(id, request));
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.ok();
    }
}
