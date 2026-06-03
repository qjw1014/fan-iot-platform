package com.faniot.platform.monitor.vo;

import java.util.List;

public record RealtimeOverviewVO(
        long onlineGateways,
        long offlineGateways,
        long onlineDevices,
        long offlineDevices,
        long alarmDevices,
        long activeAlarms,
        List<RealtimeMetricVO> latestDevices
) {
}
