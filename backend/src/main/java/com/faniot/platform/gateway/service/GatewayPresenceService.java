package com.faniot.platform.gateway.service;

import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.repository.GatewayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
public class GatewayPresenceService {

    private final GatewayRepository gatewayRepository;

    public GatewayPresenceService(GatewayRepository gatewayRepository) {
        this.gatewayRepository = gatewayRepository;
    }

    @Transactional
    public boolean markConnected(String clientId, OffsetDateTime observedAt) {
        Optional<Gateway> gateway = findGateway(clientId);
        if (gateway.isEmpty()) {
            return false;
        }

        Gateway entity = gateway.get();
        entity.setOnlineStatus("online");
        entity.setLastSeenAt(observedAt);
        gatewayRepository.save(entity);
        return true;
    }

    @Transactional
    public boolean markDisconnected(String clientId, OffsetDateTime observedAt) {
        Optional<Gateway> gateway = findGateway(clientId);
        if (gateway.isEmpty()) {
            return false;
        }

        Gateway entity = gateway.get();
        entity.setOnlineStatus("offline");
        entity.setLastSeenAt(observedAt);
        gatewayRepository.save(entity);
        return true;
    }

    @Transactional
    public void refreshConnected(Collection<String> clientIds, OffsetDateTime observedAt) {
        for (String clientId : clientIds) {
            findGateway(clientId).ifPresent(gateway -> {
                gateway.setOnlineStatus("online");
                gateway.setLastSeenAt(observedAt);
                gatewayRepository.save(gateway);
            });
        }
    }

    private Optional<Gateway> findGateway(String clientId) {
        if (!StringUtils.hasText(clientId)) {
            return Optional.empty();
        }
        return gatewayRepository.findByMqttClientId(clientId)
                .or(() -> gatewayRepository.findByGatewaySn(clientId));
    }
}
