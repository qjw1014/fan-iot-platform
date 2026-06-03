package com.faniot.platform.system.vo;

import com.faniot.platform.system.domain.SystemLog;

import java.time.OffsetDateTime;

public record SystemLogVO(
        Long id,
        String logType,
        String logLevel,
        String module,
        String operation,
        String message,
        Long userId,
        OffsetDateTime createdAt
) {
    public static SystemLogVO from(SystemLog log) {
        return new SystemLogVO(
                log.getId(),
                log.getLogType(),
                log.getLogLevel(),
                log.getModule(),
                log.getOperation(),
                log.getMessage(),
                log.getUserId(),
                log.getCreatedAt()
        );
    }
}
