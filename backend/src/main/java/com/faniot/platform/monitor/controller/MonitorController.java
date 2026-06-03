package com.faniot.platform.monitor.controller;

import com.faniot.platform.common.api.ApiResponse;
import com.faniot.platform.monitor.service.MonitorService;
import com.faniot.platform.monitor.vo.RealtimeMetricVO;
import com.faniot.platform.monitor.vo.RealtimeOverviewVO;
import com.faniot.platform.monitor.vo.TelemetryHistoryPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Tag(name = "实时监控", description = "实时监控和历史曲线")
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Operation(summary = "实时概览")
    @GetMapping("/overview")
    public ApiResponse<RealtimeOverviewVO> overview() {
        return ApiResponse.ok(monitorService.overview());
    }

    @Operation(summary = "设备最新遥测")
    @GetMapping("/latest")
    public ApiResponse<List<RealtimeMetricVO>> latest(
            @RequestParam(required = false) String deviceId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(monitorService.latest(deviceId, limit));
    }

    @Operation(summary = "设备历史曲线")
    @GetMapping("/history")
    public ApiResponse<List<TelemetryHistoryPointVO>> history(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return ApiResponse.ok(monitorService.history(deviceId, start, end, limit));
    }
}
