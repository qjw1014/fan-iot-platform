package com.faniot.platform.ai.vo;

import com.faniot.platform.monitor.vo.RealtimeMetricVO;

public record AiRealtimeVO(
        AiDeviceMetaVO device,
        RealtimeMetricVO telemetry
) {
}
