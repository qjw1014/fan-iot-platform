package com.faniot.platform.ai.vo;

import com.faniot.platform.monitor.vo.TelemetryHistoryPointVO;

import java.util.List;

public record AiHistoryVO(
        AiDeviceMetaVO device,
        List<TelemetryHistoryPointVO> records
) {
}
