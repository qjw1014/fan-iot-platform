package com.faniot.platform.ai.service;

import com.faniot.platform.ai.dto.AiExportRequest;
import com.faniot.platform.ai.vo.AiDeviceMetaVO;
import com.faniot.platform.ai.vo.AiHistoryVO;
import com.faniot.platform.ai.vo.AiRealtimeVO;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.device.service.DeviceService;
import com.faniot.platform.device.vo.DeviceVO;
import com.faniot.platform.monitor.service.MonitorService;
import com.faniot.platform.monitor.vo.RealtimeMetricVO;
import com.faniot.platform.monitor.vo.TelemetryHistoryPointVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AiOpenApiService {

    private final MonitorService monitorService;
    private final DeviceService deviceService;

    public AiOpenApiService(MonitorService monitorService, DeviceService deviceService) {
        this.monitorService = monitorService;
        this.deviceService = deviceService;
    }

    @Transactional(readOnly = true)
    public AiRealtimeVO realtime(String deviceId) {
        DeviceVO device = deviceService.detailByDeviceId(deviceId);
        List<RealtimeMetricVO> latest = monitorService.latest(deviceId, 1);
        RealtimeMetricVO telemetry = latest.isEmpty() ? null : latest.get(0);
        return new AiRealtimeVO(AiDeviceMetaVO.from(device), telemetry);
    }

    @Transactional(readOnly = true)
    public AiHistoryVO history(String deviceId, OffsetDateTime start, OffsetDateTime end, int limit) {
        DeviceVO device = deviceService.detailByDeviceId(deviceId);
        List<TelemetryHistoryPointVO> records = monitorService.history(deviceId, start, end, limit);
        return new AiHistoryVO(AiDeviceMetaVO.from(device), records);
    }

    @Transactional(readOnly = true)
    public AiHistoryVO exportData(AiExportRequest request) {
        return history(request.deviceId(), request.start(), request.end(), request.safeLimit());
    }

    public byte[] toCsv(AiHistoryVO history) {
        StringBuilder builder = new StringBuilder();
        builder.append("deviceId,deviceName,gatewaySn,customerName,projectName,latitude,longitude,address,timestamp,rpm,current,voltage,power,motorTemperature,bearingTemperature,vibration\n");
        for (TelemetryHistoryPointVO point : history.records()) {
            builder.append(csv(history.device().deviceId())).append(',')
                    .append(csv(history.device().deviceName())).append(',')
                    .append(csv(history.device().gatewaySn())).append(',')
                    .append(csv(history.device().customerName())).append(',')
                    .append(csv(history.device().projectName())).append(',')
                    .append(value(history.device().latitude())).append(',')
                    .append(value(history.device().longitude())).append(',')
                    .append(csv(history.device().address())).append(',')
                    .append(value(point.timestamp())).append(',')
                    .append(value(point.rpm())).append(',')
                    .append(value(point.current())).append(',')
                    .append(value(point.voltage())).append(',')
                    .append(value(point.power())).append(',')
                    .append(value(point.motorTemperature())).append(',')
                    .append(value(point.bearingTemperature())).append(',')
                    .append(value(point.vibration())).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public void ensureFormat(String format) {
        if (!"json".equals(format) && !"csv".equals(format)) {
            throw new BusinessException("导出格式仅支持json或csv");
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String value(Object value) {
        return value == null ? "" : value.toString();
    }
}
