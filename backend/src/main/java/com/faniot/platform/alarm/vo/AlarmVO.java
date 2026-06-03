package com.faniot.platform.alarm.vo;

import java.time.OffsetDateTime;

public record AlarmVO(
        Long id,
        String alarmId,
        String gatewayId,
        String deviceId,
        String alarmType,
        String alarmLevel,
        String alarmCode,
        String alarmMessage,
        OffsetDateTime occurredAt,
        OffsetDateTime recoveredAt,
        Boolean acknowledged,
        OffsetDateTime acknowledgedAt,
        String status,
        OffsetDateTime createdAt
) {
}
