package com.faniot.platform.location.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.location.config.LbsProperties;
import com.faniot.platform.location.dto.LbsLocationRequest;
import com.faniot.platform.location.vo.LbsLocationVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class LbsLocationService {

    private final LbsProperties properties;
    private final LbsProviderClient providerClient;
    private final GatewayRepository gatewayRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, AutomaticLocationAttempt> automaticAttempts = new ConcurrentHashMap<>();

    public LbsLocationService(
            LbsProperties properties,
            LbsProviderClient providerClient,
            GatewayRepository gatewayRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.providerClient = providerClient;
        this.gatewayRepository = gatewayRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public LbsLocationVO locate(LbsLocationRequest request) {
        Gateway gateway = findGateway(request.gatewaySn(), request.imei());
        int mcc = request.mcc() == null ? properties.defaultMcc() : request.mcc();
        Integer configuredMnc = request.mnc() == null ? properties.defaultMnc() : request.mnc();
        if (configuredMnc == null) {
            throw new BusinessException("缺少MNC，请在报文中提供或配置LBS_DEFAULT_MNC");
        }
        if (!properties.enabled()) {
            saveLog(gateway, mcc, configuredMnc, request.lac(), request.cid(), "skipped", null, "基站定位服务未启用");
            throw new BusinessException("基站定位服务未启用，请设置LBS_ENABLED=true");
        }

        LbsProviderClient.Result result;
        try {
            result = providerClient.locate(mcc, configuredMnc, request.lac(), request.cid(), gateway.getImei());
        } catch (RuntimeException ex) {
            saveLog(gateway, mcc, configuredMnc, request.lac(), request.cid(), "failed", null, ex.getMessage());
            throw ex;
        }

        OffsetDateTime locatedAt = OffsetDateTime.now();
        boolean hasManualLocation = "manual".equals(gateway.getLocationSource())
                && gateway.getLatitude() != null
                && gateway.getLongitude() != null;
        gateway.setLbsMcc(mcc);
        gateway.setLbsMnc(configuredMnc);
        gateway.setLbsLac(request.lac());
        gateway.setLbsCid(request.cid());
        gateway.setLbsAccuracy(result.accuracy());
        if (!hasManualLocation) {
            gateway.setLatitude(result.latitude());
            gateway.setLongitude(result.longitude());
            gateway.setAddress(result.address());
            gateway.setProvince(result.province());
            gateway.setCity(result.city());
            gateway.setDistrict(result.district());
            gateway.setLocationSource("lbs");
            gateway.setLocationUpdatedAt(locatedAt);
            gateway.setLastLocationTime(locatedAt);
        }
        gatewayRepository.save(gateway);
        saveLog(gateway, mcc, configuredMnc, request.lac(), request.cid(), "success", result, null);

        return new LbsLocationVO(
                gateway.getGatewayId(),
                gateway.getGatewaySn(),
                mcc,
                configuredMnc,
                request.lac(),
                request.cid(),
                result.latitude(),
                result.longitude(),
                result.accuracy(),
                result.address(),
                result.province(),
                result.city(),
                result.district(),
                "lbs",
                !hasManualLocation,
                locatedAt
        );
    }

    public void locateFromTelemetry(LbsLocationRequest request) {
        Gateway gateway = findGateway(request.gatewaySn(), request.imei());
        int mcc = request.mcc() == null ? properties.defaultMcc() : request.mcc();
        Integer mnc = request.mnc() == null ? properties.defaultMnc() : request.mnc();
        if (mnc == null) {
            throw new BusinessException("缺少MNC，请在报文中提供或配置LBS_DEFAULT_MNC");
        }

        AutomaticLocationAttempt next = new AutomaticLocationAttempt(
                mcc,
                mnc,
                request.lac(),
                request.cid(),
                OffsetDateTime.now(),
                false
        );
        AutomaticLocationAttempt previous = automaticAttempts.get(gateway.getGatewayId());
        int intervalMinutes = previous != null && previous.success()
                ? properties.refreshIntervalMinutes()
                : Math.min(5, properties.refreshIntervalMinutes());
        if (previous != null
                && previous.sameCell(next)
                && previous.at().isAfter(OffsetDateTime.now().minusMinutes(intervalMinutes))) {
            return;
        }

        automaticAttempts.put(gateway.getGatewayId(), next);
        locate(request);
        automaticAttempts.put(
                gateway.getGatewayId(),
                new AutomaticLocationAttempt(mcc, mnc, request.lac(), request.cid(), OffsetDateTime.now(), true)
        );
    }

    private Gateway findGateway(String gatewaySn, String imei) {
        if (StringUtils.hasText(gatewaySn)) {
            return gatewayRepository.findByGatewaySn(gatewaySn)
                    .orElseThrow(() -> new BusinessException("未找到对应SN的盒子"));
        }
        if (StringUtils.hasText(imei)) {
            return gatewayRepository.findByImei(imei)
                    .orElseThrow(() -> new BusinessException("未找到对应IMEI的盒子"));
        }
        throw new BusinessException("gatewaySn和imei至少提供一个");
    }

    private void saveLog(
            Gateway gateway,
            int mcc,
            int mnc,
            long lac,
            long cid,
            String status,
            LbsProviderClient.Result result,
            String error
    ) {
        jdbcTemplate.update("""
                        insert into d200_lbs_location_logs (
                            gateway_id, gateway_sn, imei, mcc, mnc, lac, cid, provider, status,
                            latitude, longitude, accuracy, address, error_message, raw_response, located_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), ?)
                        """,
                gateway.getGatewayId(),
                gateway.getGatewaySn(),
                gateway.getImei(),
                mcc,
                mnc,
                lac,
                cid,
                properties.provider(),
                status,
                result == null ? null : result.latitude(),
                result == null ? null : result.longitude(),
                result == null ? null : result.accuracy(),
                result == null ? null : result.address(),
                error,
                result == null ? null : jsonValue(result.rawResponse()),
                result == null ? null : OffsetDateTime.now()
        );
    }

    private String jsonValue(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            objectMapper.readTree(raw);
            return raw;
        } catch (JsonProcessingException ignored) {
            try {
                return objectMapper.writeValueAsString(raw);
            } catch (JsonProcessingException ex) {
                return null;
            }
        }
    }

    private record AutomaticLocationAttempt(
            int mcc,
            int mnc,
            long lac,
            long cid,
            OffsetDateTime at,
            boolean success
    ) {
        private boolean sameCell(AutomaticLocationAttempt other) {
            return other != null
                    && mcc == other.mcc
                    && mnc == other.mnc
                    && lac == other.lac
                    && cid == other.cid;
        }
    }
}
