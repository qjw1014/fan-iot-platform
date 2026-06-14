package com.faniot.platform.iot.mqtt;

import com.faniot.platform.gateway.service.GatewayPresenceService;
import com.faniot.platform.iot.config.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MqttGatewaySessionTracker {

    private static final Logger log = LoggerFactory.getLogger(MqttGatewaySessionTracker.class);
    private static final Pattern CONNECTED = Pattern.compile(
            "New client connected from .+ as ([^ ]+) \\(.*\\)\\.?$"
    );
    private static final Pattern DISCONNECTED = Pattern.compile(
            "Client ([^ ]+)(?: \\[[^]]+])? disconnected(?:: (.*?))?\\.?$"
    );
    private static final Pattern CLOSED = Pattern.compile(
            "Client ([^ ]+) closed its connection\\.?$"
    );
    private static final Pattern TIMESTAMP = Pattern.compile("^\\s*(\\d{10,}):\\s*");

    private final GatewayPresenceService gatewayPresenceService;
    private final MqttProperties properties;
    private final Set<String> activeClientIds = ConcurrentHashMap.newKeySet();
    private volatile boolean brokerAvailable;

    public MqttGatewaySessionTracker(
            GatewayPresenceService gatewayPresenceService,
            MqttProperties properties
    ) {
        this.gatewayPresenceService = gatewayPresenceService;
        this.properties = properties;
    }

    public void brokerConnected() {
        brokerAvailable = true;
        reconcileBrokerLog();
    }

    public void brokerDisconnected() {
        brokerAvailable = false;
    }

    public void handleBrokerEvent(String payload) {
        handleBrokerEvent(payload, eventTime(payload));
    }

    void reconcileBrokerLog() {
        String configuredPath = properties.getBrokerLogPath();
        if (!StringUtils.hasText(configuredPath)) {
            return;
        }

        Path path = Path.of(configuredPath);
        if (!Files.isRegularFile(path)) {
            log.warn("MQTT broker log is unavailable for session recovery: {}", path);
            return;
        }

        try {
            String tail = readTail(path, properties.getBrokerLogTailBytes());
            for (String line : tail.split("\\R")) {
                if (StringUtils.hasText(line)) {
                    handleBrokerEvent(line, eventTime(line));
                }
            }
            log.info(
                    "MQTT gateway sessions recovered from broker log: activeClients={}, path={}",
                    activeClientIds.size(),
                    path
            );
        } catch (IOException ex) {
            log.warn("MQTT broker log session recovery failed: path={}, message={}", path, ex.getMessage());
        }
    }

    private void handleBrokerEvent(String payload, OffsetDateTime occurredAt) {
        String event = payload == null ? "" : payload.trim();
        Matcher connected = CONNECTED.matcher(event);
        if (connected.find()) {
            String clientId = connected.group(1);
            if (gatewayPresenceService.markConnected(clientId, occurredAt)) {
                activeClientIds.add(clientId);
                log.info("Gateway MQTT session connected: clientId={}", clientId);
            }
            return;
        }

        Matcher disconnected = DISCONNECTED.matcher(event);
        if (disconnected.find()) {
            String reason = disconnected.group(2);
            if (reason != null && reason.contains("session taken over")) {
                return;
            }
            markDisconnected(disconnected.group(1), occurredAt);
            return;
        }

        Matcher closed = CLOSED.matcher(event);
        if (closed.find()) {
            markDisconnected(closed.group(1), occurredAt);
        }
    }

    @Scheduled(fixedDelayString = "${app.mqtt.session-refresh-interval-ms:30000}")
    public void refreshConnectedGateways() {
        if (!brokerAvailable || activeClientIds.isEmpty()) {
            return;
        }
        gatewayPresenceService.refreshConnected(Set.copyOf(activeClientIds), OffsetDateTime.now());
    }

    private void markDisconnected(String clientId, OffsetDateTime occurredAt) {
        activeClientIds.remove(clientId);
        if (gatewayPresenceService.markDisconnected(clientId, occurredAt)) {
            log.info("Gateway MQTT session disconnected: clientId={}", clientId);
        }
    }

    private OffsetDateTime eventTime(String event) {
        Matcher matcher = TIMESTAMP.matcher(event == null ? "" : event);
        if (!matcher.find()) {
            return OffsetDateTime.now();
        }
        try {
            return OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(matcher.group(1))),
                    ZoneOffset.UTC
            );
        } catch (RuntimeException ex) {
            return OffsetDateTime.now();
        }
    }

    private String readTail(Path path, long configuredBytes) throws IOException {
        long tailBytes = Math.min(Integer.MAX_VALUE, Math.max(1024, configuredBytes));
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long length = file.length();
            long start = Math.max(0, length - tailBytes);
            file.seek(start);
            byte[] bytes = new byte[(int) (length - start)];
            file.readFully(bytes);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (start == 0) {
                return content;
            }
            int firstLineBreak = content.indexOf('\n');
            return firstLineBreak >= 0 ? content.substring(firstLineBreak + 1) : "";
        }
    }
}
