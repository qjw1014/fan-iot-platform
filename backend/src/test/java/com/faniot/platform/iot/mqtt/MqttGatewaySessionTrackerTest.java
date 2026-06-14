package com.faniot.platform.iot.mqtt;

import com.faniot.platform.gateway.service.GatewayPresenceService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttGatewaySessionTrackerTest {

    private final GatewayPresenceService presenceService = mock(GatewayPresenceService.class);
    private final MqttGatewaySessionTracker tracker = new MqttGatewaySessionTracker(presenceService);

    @Test
    void marksKnownGatewayOnlineWhenBrokerReportsConnection() {
        when(presenceService.markConnected(any(), any())).thenReturn(true);

        tracker.handleBrokerEvent(
                "1781424082: New client connected from 117.61.110.182:8785 as 00100326052100060534 (p4, c1, k60, u'd200')."
        );

        verify(presenceService).markConnected(eq("00100326052100060534"), any(OffsetDateTime.class));
    }

    @Test
    void marksGatewayOfflineWhenSessionCloses() {
        when(presenceService.markDisconnected(any(), any())).thenReturn(true);

        tracker.handleBrokerEvent(
                "Client 00100326052100060534 [117.61.110.182:8785] disconnected: exceeded timeout."
        );

        verify(presenceService).markDisconnected(eq("00100326052100060534"), any(OffsetDateTime.class));
    }

    @Test
    void ignoresOldSessionDisconnectDuringClientTakeover() {
        tracker.handleBrokerEvent(
                "Client 00100326052100060534 [117.61.110.182:8785] disconnected: session taken over."
        );

        verify(presenceService, never()).markDisconnected(any(), any());
    }
}
