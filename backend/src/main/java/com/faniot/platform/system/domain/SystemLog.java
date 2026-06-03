package com.faniot.platform.system.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_logs")
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_type", nullable = false)
    private String logType;

    @Column(name = "log_level", nullable = false)
    private String logLevel;

    private String module;

    private String operation;

    @Column(nullable = false)
    private String message;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getLogType() {
        return logType;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getModule() {
        return module;
    }

    public String getOperation() {
        return operation;
    }

    public String getMessage() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
