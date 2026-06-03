package com.faniot.platform.ai.controller;

import com.faniot.platform.ai.dto.AiExportRequest;
import com.faniot.platform.ai.service.AiApiCallLogService;
import com.faniot.platform.ai.service.AiApiKeyContext;
import com.faniot.platform.ai.service.AiApiKeyService;
import com.faniot.platform.ai.service.AiOpenApiService;
import com.faniot.platform.ai.vo.AiHistoryVO;
import com.faniot.platform.ai.vo.AiRealtimeVO;
import com.faniot.platform.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@Tag(name = "AI开放接口", description = "面向第三方AI、大模型和数据分析系统的标准数据接口")
@RestController
@RequestMapping("/api/ai")
public class AiOpenApiController {

    private final AiApiKeyService aiApiKeyService;
    private final AiOpenApiService aiOpenApiService;
    private final AiApiCallLogService callLogService;

    public AiOpenApiController(AiApiKeyService aiApiKeyService, AiOpenApiService aiOpenApiService, AiApiCallLogService callLogService) {
        this.aiApiKeyService = aiApiKeyService;
        this.aiOpenApiService = aiOpenApiService;
        this.callLogService = callLogService;
    }

    @Operation(summary = "AI实时数据")
    @GetMapping("/realtime/{deviceId}")
    public ApiResponse<AiRealtimeVO> realtime(
            @PathVariable String deviceId,
            @RequestHeader(value = "X-API-Key", required = false) String xApiKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request
    ) {
        long startedAt = System.currentTimeMillis();
        AiApiKeyContext context = authenticate(xApiKey, authorization);
        try {
            AiRealtimeVO data = aiOpenApiService.realtime(deviceId);
            callLogService.log(request, context, Map.of("deviceId", deviceId), "json", 200, true, null, startedAt);
            return ApiResponse.ok(data);
        } catch (RuntimeException ex) {
            callLogService.log(request, context, Map.of("deviceId", deviceId), "json", 500, false, ex.getMessage(), startedAt);
            throw ex;
        }
    }

    @Operation(summary = "AI历史数据")
    @GetMapping("/history")
    public ApiResponse<AiHistoryVO> history(
            @RequestParam String deviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end,
            @RequestParam(defaultValue = "500") int limit,
            @RequestHeader(value = "X-API-Key", required = false) String xApiKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request
    ) {
        long startedAt = System.currentTimeMillis();
        AiApiKeyContext context = authenticate(xApiKey, authorization);
        Map<String, Object> params = Map.of("deviceId", deviceId, "limit", limit);
        try {
            AiHistoryVO data = aiOpenApiService.history(deviceId, start, end, limit);
            callLogService.log(request, context, params, "json", 200, true, null, startedAt);
            return ApiResponse.ok(data);
        } catch (RuntimeException ex) {
            callLogService.log(request, context, params, "json", 500, false, ex.getMessage(), startedAt);
            throw ex;
        }
    }

    @Operation(summary = "AI数据导出")
    @PostMapping("/export")
    public ResponseEntity<?> export(
            @Valid @RequestBody AiExportRequest exportRequest,
            @RequestHeader(value = "X-API-Key", required = false) String xApiKey,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest request
    ) {
        long startedAt = System.currentTimeMillis();
        AiApiKeyContext context = authenticate(xApiKey, authorization);
        String format = exportRequest.safeFormat();
        aiOpenApiService.ensureFormat(format);
        try {
            AiHistoryVO data = aiOpenApiService.exportData(exportRequest);
            callLogService.log(request, context, exportRequest, format, 200, true, null, startedAt);
            if ("csv".equals(format)) {
                String filename = "fan-iot-" + exportRequest.deviceId() + ".csv";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                        .contentType(new MediaType("text", "csv"))
                        .body(aiOpenApiService.toCsv(data));
            }
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (RuntimeException ex) {
            callLogService.log(request, context, exportRequest, format, 500, false, ex.getMessage(), startedAt);
            throw ex;
        }
    }

    private AiApiKeyContext authenticate(String xApiKey, String authorization) {
        return aiApiKeyService.authenticate(resolveApiKey(xApiKey, authorization));
    }

    private String resolveApiKey(String xApiKey, String authorization) {
        if (StringUtils.hasText(xApiKey)) {
            return xApiKey;
        }
        if (StringUtils.hasText(authorization) && authorization.startsWith("ApiKey ")) {
            return authorization.substring("ApiKey ".length());
        }
        return null;
    }
}
