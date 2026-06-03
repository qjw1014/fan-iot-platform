package com.faniot.platform.gateway.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.gateway.dto.GatewayRequest;
import com.faniot.platform.gateway.service.GatewayService;
import com.faniot.platform.gateway.vo.GatewayVO;
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

import java.util.List;

@Tag(name = "盒子管理", description = "物联网盒子新增、编辑、删除和分页查询")
@RestController
@RequestMapping("/api/gateways")
public class GatewayController {

    private final GatewayService gatewayService;

    public GatewayController(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Operation(summary = "分页查询盒子")
    @GetMapping
    public ApiResponse<PageResponse<GatewayVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String projectId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(gatewayService.page(keyword, customerId, projectId, page, size));
    }

    @Operation(summary = "盒子下拉列表")
    @GetMapping("/options")
    public ApiResponse<List<GatewayVO>> options(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String projectId
    ) {
        return ApiResponse.ok(gatewayService.list(customerId, projectId));
    }

    @Operation(summary = "盒子详情")
    @GetMapping("/{id}")
    public ApiResponse<GatewayVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(gatewayService.detail(id));
    }

    @Operation(summary = "新增盒子")
    @PostMapping
    public ApiResponse<GatewayVO> create(@Valid @RequestBody GatewayRequest request) {
        return ApiResponse.ok(gatewayService.create(request));
    }

    @Operation(summary = "编辑盒子")
    @PutMapping("/{id}")
    public ApiResponse<GatewayVO> update(@PathVariable Long id, @Valid @RequestBody GatewayRequest request) {
        return ApiResponse.ok(gatewayService.update(id, request));
    }

    @Operation(summary = "删除盒子")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        gatewayService.delete(id);
        return ApiResponse.ok();
    }
}
