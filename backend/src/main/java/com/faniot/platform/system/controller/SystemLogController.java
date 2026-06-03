package com.faniot.platform.system.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.system.service.SystemLogService;
import com.faniot.platform.system.vo.SystemLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "系统日志", description = "系统日志分页查询")
@RestController
@RequestMapping("/api/system/logs")
public class SystemLogController {

    private final SystemLogService systemLogService;

    public SystemLogController(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Operation(summary = "分页查询系统日志")
    @GetMapping
    public ApiResponse<PageResponse<SystemLogVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(systemLogService.page(keyword, logLevel, module, page, size));
    }
}
