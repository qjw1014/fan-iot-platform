package com.faniot.platform.d200.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.d200.dto.D200ConfigTaskRequest;
import com.faniot.platform.d200.dto.D200FieldMappingRequest;
import com.faniot.platform.d200.service.D200AdapterService;
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

import java.util.Map;

@Tag(name = "D200专项适配", description = "D200原始报文、字段映射和远程配置任务")
@RestController
@RequestMapping("/api/d200")
public class D200Controller {

    private final D200AdapterService d200AdapterService;

    public D200Controller(D200AdapterService d200AdapterService) {
        this.d200AdapterService = d200AdapterService;
    }

    @Operation(summary = "D200原始报文分页")
    @GetMapping("/raw-payloads")
    public ApiResponse<PageResponse<Map<String, Object>>> rawPayloads(
            @RequestParam(required = false) String gatewaySn,
            @RequestParam(required = false) Boolean processed,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(d200AdapterService.rawPayloads(gatewaySn, processed, page, size));
    }

    @Operation(summary = "D200字段映射分页")
    @GetMapping("/field-mappings")
    public ApiResponse<PageResponse<Map<String, Object>>> mappings(
            @RequestParam(required = false) String gatewayId,
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(d200AdapterService.mappings(gatewayId, deviceId, page, size));
    }

    @Operation(summary = "新增D200字段映射")
    @PostMapping("/field-mappings")
    public ApiResponse<Map<String, Object>> createMapping(@Valid @RequestBody D200FieldMappingRequest request) {
        return ApiResponse.ok(d200AdapterService.createMapping(request));
    }

    @Operation(summary = "编辑D200字段映射")
    @PutMapping("/field-mappings/{id}")
    public ApiResponse<Map<String, Object>> updateMapping(@PathVariable Long id, @Valid @RequestBody D200FieldMappingRequest request) {
        return ApiResponse.ok(d200AdapterService.updateMapping(id, request));
    }

    @Operation(summary = "删除D200字段映射")
    @DeleteMapping("/field-mappings/{id}")
    public ApiResponse<Void> deleteMapping(@PathVariable Long id) {
        d200AdapterService.deleteMapping(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "D200远程配置任务分页")
    @GetMapping("/config-tasks")
    public ApiResponse<PageResponse<Map<String, Object>>> configTasks(
            @RequestParam(required = false) String gatewaySn,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(d200AdapterService.configTasks(gatewaySn, status, page, size));
    }

    @Operation(summary = "创建D200远程配置任务")
    @PostMapping("/config-tasks")
    public ApiResponse<Map<String, Object>> createConfigTask(@Valid @RequestBody D200ConfigTaskRequest request) {
        return ApiResponse.ok(d200AdapterService.createConfigTask(request));
    }

    @Operation(summary = "更新D200远程配置任务状态")
    @PostMapping("/config-tasks/{id}/status")
    public ApiResponse<Void> markConfigTask(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String errorMessage
    ) {
        d200AdapterService.markConfigTask(id, status, errorMessage);
        return ApiResponse.ok();
    }
}
