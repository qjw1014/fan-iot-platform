package com.faniot.platform.monitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CommunicationStatusService {

    private static final Logger log = LoggerFactory.getLogger(CommunicationStatusService.class);

    private final JdbcTemplate jdbcTemplate;
    private final long offlineTimeoutSeconds;

    public CommunicationStatusService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.communication.offline-timeout-seconds:180}") long offlineTimeoutSeconds
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.offlineTimeoutSeconds = Math.max(offlineTimeoutSeconds, 30);
    }

    @Scheduled(fixedDelayString = "${app.communication.status-check-interval-ms:30000}")
    public void markTimedOutConnectionsOffline() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusSeconds(offlineTimeoutSeconds);

        int gateways = jdbcTemplate.update("""
                update gateways
                   set online_status = 'offline',
                       updated_at = now()
                 where online_status = 'online'
                   and (last_seen_at is null or last_seen_at < ?)
                """, cutoff);

        int devices = jdbcTemplate.update("""
                update devices
                   set status = 'offline',
                       updated_at = now()
                 where status in ('online', 'alarm')
                   and (last_seen_at is null or last_seen_at < ?)
                """, cutoff);

        if (gateways > 0 || devices > 0) {
            log.info("Communication timeout detected: gateways={}, devices={}, timeoutSeconds={}",
                    gateways, devices, offlineTimeoutSeconds);
        }
    }
}
