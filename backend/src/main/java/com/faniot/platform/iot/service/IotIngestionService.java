package com.faniot.platform.iot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.device.domain.Device;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.iot.dto.GatewayActivationRequest;
import com.faniot.platform.iot.dto.GatewayActivationResponse;
import com.faniot.platform.iot.dto.TelemetryPointRequest;
import com.faniot.platform.iot.dto.TelemetryUploadRequest;
import com.faniot.platform.iot.dto.TelemetryUploadResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class IotIngestionService {

    private static final BigDecimal TEMP_WARNING = new BigDecimal("80.0001");
    private static final BigDecimal TEMP_CRITICAL = new BigDecimal("85");
    private static final BigDecimal VIBRATION_WARNING = new BigDecimal("5.0001");
    private static final BigDecimal VIBRATION_CRITICAL = new BigDecimal("5.0");
    private static final BigDecimal CURRENT_WARNING = new BigDecimal("16");
    private static final BigDecimal CURRENT_CRITICAL = new BigDecimal("22");

    private final GatewayRepository gatewayRepository;
    private final DeviceRepository deviceRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public IotIngestionService(
            GatewayRepository gatewayRepository,
            DeviceRepository deviceRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.gatewayRepository = gatewayRepository;
        this.deviceRepository = deviceRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GatewayActivationResponse activate(GatewayActivationRequest request) {
        Gateway gateway = gatewayRepository.findByGatewayId(request.gatewayId())
                .or(() -> gatewayRepository.findByGatewaySn(request.gatewaySn()))
                .orElseGet(Gateway::new);

        if (StringUtils.hasText(gateway.getGatewayId()) && !gateway.getGatewayId().equals(request.gatewayId())) {
            throw new BusinessException("盒子编号与序列号不匹配");
        }
        if (StringUtils.hasText(gateway.getGatewaySn()) && !gateway.getGatewaySn().equals(request.gatewaySn())) {
            throw new BusinessException("盒子序列号与编号不匹配");
        }

        gateway.setGatewayId(request.gatewayId());
        gateway.setGatewaySn(request.gatewaySn());
        if (!StringUtils.hasText(gateway.getGatewayName())) {
            gateway.setGatewayName(request.gatewayId());
        }
        if (!StringUtils.hasText(gateway.getGatewayModel())) {
            gateway.setGatewayModel("D200");
        }
        gateway.setMqttClientId(request.gatewaySn());
        gateway.setPublishTopic("iot/d200/" + request.gatewaySn() + "/up");
        gateway.setSubscribeTopic("iot/d200/" + request.gatewaySn() + "/down");
        gateway.setMqttVersion("3.1.1");
        gateway.setQos(1);
        gateway.setKeepalive(60);
        gateway.setTlsEnabled(false);
        gateway.setRemoteConfigSupported(true);
        gateway.setActivationStatus("active");
        gateway.setOnlineStatus("online");
        gateway.setMqttUsername(request.mqttUsername());
        gateway.setMqttPasswordHash(passwordEncoder.encode(request.mqttPassword()));
        gateway.setLastSeenAt(OffsetDateTime.now());
        Gateway saved = gatewayRepository.save(gateway);

        return new GatewayActivationResponse(
                saved.getGatewayId(),
                saved.getGatewaySn(),
                saved.getActivationStatus(),
                saved.getMqttUsername(),
                saved.getPublishTopic()
        );
    }

    @Transactional
    public TelemetryUploadResponse ingestTelemetry(TelemetryUploadRequest request, String topicGatewayId) {
        if (StringUtils.hasText(topicGatewayId) && !topicGatewayId.equals(request.gatewayId())) {
            throw new BusinessException("MQTT主题中的盒子编号与消息体不一致");
        }
        Gateway gateway = validateGatewayCredential(request);
        List<TelemetryPointRequest> points = request.data();
        if (CollectionUtils.isEmpty(points)) {
            throw new BusinessException("遥测数据不能为空");
        }

        int savedCount = 0;
        OffsetDateTime now = OffsetDateTime.now();
        for (TelemetryPointRequest point : points) {
            savePoint(gateway, point, now);
            savedCount++;
        }

        gateway.setOnlineStatus("online");
        gateway.setLastSeenAt(now);
        gatewayRepository.save(gateway);

        return new TelemetryUploadResponse(gateway.getGatewayId(), savedCount);
    }

    private Gateway validateGatewayCredential(TelemetryUploadRequest request) {
        Gateway gateway = gatewayRepository.findByGatewayId(request.gatewayId())
                .orElseThrow(() -> new BusinessException("盒子不存在，请先激活"));
        if (!"active".equals(gateway.getActivationStatus())) {
            throw new BusinessException("盒子未激活或已禁用");
        }
        if (!request.gatewaySn().equals(gateway.getGatewaySn())) {
            throw new BusinessException("盒子序列号不正确");
        }
        if (!request.mqttUsername().equals(gateway.getMqttUsername())) {
            throw new BusinessException("MQTT用户名不正确");
        }
        if (!StringUtils.hasText(gateway.getMqttPasswordHash())
                || !passwordEncoder.matches(request.mqttPassword(), gateway.getMqttPasswordHash())) {
            throw new BusinessException("MQTT密码不正确");
        }
        return gateway;
    }

    public void ingestStandardPoint(Gateway gateway, TelemetryPointRequest point, OffsetDateTime receivedAt) {
        savePoint(gateway, point, receivedAt);
    }

    private void savePoint(Gateway gateway, TelemetryPointRequest point, OffsetDateTime receivedAt) {
        Device device = deviceRepository.findByDeviceId(point.deviceId())
                .orElseThrow(() -> new BusinessException("设备不存在：" + point.deviceId()));
        if (!gateway.getGatewayId().equals(device.getGatewayId())) {
            throw new BusinessException("设备不属于当前盒子：" + point.deviceId());
        }

        OffsetDateTime timestamp = point.timestamp() == null ? receivedAt : point.timestamp();
        String telemetryStatus = StringUtils.hasText(point.status()) ? point.status() : "normal";
        String rawPayload = toJson(point);

        jdbcTemplate.update("""
                        INSERT INTO device_telemetry (
                            gateway_id, device_id, timestamp, rpm, current, voltage, power, frequency,
                            pressure, airflow, motor_temperature, bearing_temperature, vibration,
                            status, alarm_code, raw_payload, received_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
                        """,
                gateway.getGatewayId(),
                point.deviceId(),
                timestamp,
                point.rpm(),
                point.current(),
                point.voltage(),
                point.power(),
                point.frequency(),
                point.pressure(),
                point.airflow(),
                point.motorTemperature(),
                point.bearingTemperature(),
                point.vibration(),
                telemetryStatus,
                point.alarmCode(),
                rawPayload,
                receivedAt
        );

        device.setLastSeenAt(receivedAt);
        device.setStatus(toDeviceStatus(telemetryStatus));
        deviceRepository.save(device);
        createAlarms(gateway, device, point, timestamp, telemetryStatus);
    }

    private void createAlarms(Gateway gateway, Device device, TelemetryPointRequest point, OffsetDateTime occurredAt, String status) {
        BigDecimal temperature = max(point.motorTemperature(), point.bearingTemperature());
        if (temperature != null && temperature.compareTo(TEMP_WARNING) >= 0) {
            String level = temperature.compareTo(TEMP_CRITICAL) >= 0 ? "critical" : "warning";
            insertAlarm(gateway.getGatewayId(), device.getDeviceId(), "temperature_over_limit", level, point.alarmCode(),
                    device.getDeviceName() + " 温度超限：" + temperature, occurredAt);
        }
        if (point.vibration() != null && point.vibration().compareTo(VIBRATION_WARNING) >= 0) {
            String level = point.vibration().compareTo(VIBRATION_CRITICAL) >= 0 ? "critical" : "warning";
            insertAlarm(gateway.getGatewayId(), device.getDeviceId(), "vibration_over_limit", level, point.alarmCode(),
                    device.getDeviceName() + " 振动超限：" + point.vibration(), occurredAt);
        }
        if (point.current() != null && point.current().compareTo(CURRENT_WARNING) >= 0) {
            String level = point.current().compareTo(CURRENT_CRITICAL) >= 0 ? "critical" : "warning";
            insertAlarm(gateway.getGatewayId(), device.getDeviceId(), "current_over_limit", level, point.alarmCode(),
                    device.getDeviceName() + " 电流超限：" + point.current(), occurredAt);
        }
        if ("offline".equals(status)) {
            insertAlarm(gateway.getGatewayId(), device.getDeviceId(), "device_offline", "critical", point.alarmCode(),
                    device.getDeviceName() + " 设备离线", occurredAt);
        } else if ("fault".equals(status) || "critical".equals(status)) {
            insertAlarm(gateway.getGatewayId(), device.getDeviceId(), "communication_interrupted", "critical", point.alarmCode(),
                    device.getDeviceName() + " 通讯或设备故障", occurredAt);
        }
    }

    private void insertAlarm(String gatewayId, String deviceId, String type, String level, String code, String message, OffsetDateTime occurredAt) {
        jdbcTemplate.update("""
                        INSERT INTO alarms (
                            gateway_id, device_id, alarm_type, alarm_level, alarm_code,
                            alarm_message, occurred_at, status
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
                        """,
                gatewayId, deviceId, type, level, code, message, occurredAt
        );
    }

    private BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.max(right);
    }

    private String toJson(TelemetryPointRequest point) {
        try {
            return objectMapper.writeValueAsString(point);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("遥测原始数据序列化失败");
        }
    }

    private String toDeviceStatus(String telemetryStatus) {
        return switch (telemetryStatus) {
            case "critical", "warning", "fault" -> "alarm";
            case "offline" -> "offline";
            default -> "online";
        };
    }
}
