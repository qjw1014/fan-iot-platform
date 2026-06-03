package com.faniot.platform.alarm.controller;

import com.faniot.platform.alarm.service.AlarmService;
import com.faniot.platform.alarm.vo.AlarmVO;
import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "告警中心", description = "告警分页、确认和关闭")
@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @Operation(summary = "分页查询告警")
    @GetMapping
    public ApiResponse<PageResponse<AlarmVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String alarmLevel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(alarmService.page(keyword, alarmLevel, status, page, size));
    }

    @Operation(summary = "确认告警")
    @PostMapping("/{id}/acknowledge")
    public ApiResponse<Void> acknowledge(@PathVariable Long id) {
        alarmService.acknowledge(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "关闭告警")
    @PostMapping("/{id}/close")
    public ApiResponse<Void> close(@PathVariable Long id) {
        alarmService.close(id);
        return ApiResponse.ok();
    }
}
