package com.faniot.platform.iot.mqtt;

import com.faniot.platform.gateway.service.GatewayPresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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

    private final GatewayPresenceService gatewayPresenceService;
    private final Set<String> activeClientIds = ConcurrentHashMap.newKeySet();
    private volatile boolean brokerAvailable;

    public MqttGatewaySessionTracker(GatewayPresenceService gatewayPresenceService) {
        this.gatewayPresenceService = gatewayPresenceService;
    }

    public void brokerConnected() {
        brokerAvailable = true;
    }

    public void brokerDisconnected() {
        brokerAvailable = false;
    }

    public void handleBrokerEvent(String payload) {
        String event = payload == null ? "" : payload.trim();
        Matcher connected = CONNECTED.matcher(event);
        if (connected.find()) {
            String clientId = connected.group(1);
            if (gatewayPresenceService.markConnected(clientId, OffsetDateTime.now())) {
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
            markDisconnected(disconnected.group(1));
            return;
        }

        Matcher closed = CLOSED.matcher(event);
        if (closed.find()) {
            markDisconnected(closed.group(1));
        }
    }

    @Scheduled(fixedDelayString = "${app.mqtt.session-refresh-interval-ms:30000}")
    public void refreshConnectedGateways() {
        if (!brokerAvailable || activeClientIds.isEmpty()) {
            return;
        }
        gatewayPresenceService.refreshConnected(Set.copyOf(activeClientIds), OffsetDateTime.now());
    }

    private void markDisconnected(String clientId) {
        activeClientIds.remove(clientId);
        if (gatewayPresenceService.markDisconnected(clientId, OffsetDateTime.now())) {
            log.info("Gateway MQTT session disconnected: clientId={}", clientId);
        }
    }
}
