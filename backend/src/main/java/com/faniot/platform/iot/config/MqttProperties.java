package com.faniot.platform.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mqtt")
public class MqttProperties {

    private boolean enabled = true;
    private String brokerUrl = "tcp://localhost:1883";
    private String clientId = "fan-iot-platform-backend";
    private String username;
    private String password;
    private String telemetryTopic = "iot/d200/+/up";
    private String sessionEventTopic = "$SYS/broker/log/#";
    private String brokerLogPath;
    private long brokerLogTailBytes = 16 * 1024 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelemetryTopic() {
        return telemetryTopic;
    }

    public void setTelemetryTopic(String telemetryTopic) {
        this.telemetryTopic = telemetryTopic;
    }

    public String getSessionEventTopic() {
        return sessionEventTopic;
    }

    public void setSessionEventTopic(String sessionEventTopic) {
        this.sessionEventTopic = sessionEventTopic;
    }

    public String getBrokerLogPath() {
        return brokerLogPath;
    }

    public void setBrokerLogPath(String brokerLogPath) {
        this.brokerLogPath = brokerLogPath;
    }

    public long getBrokerLogTailBytes() {
        return brokerLogTailBytes;
    }

    public void setBrokerLogTailBytes(long brokerLogTailBytes) {
        this.brokerLogTailBytes = brokerLogTailBytes;
    }
}
