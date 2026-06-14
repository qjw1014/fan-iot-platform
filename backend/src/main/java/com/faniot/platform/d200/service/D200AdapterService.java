package com.faniot.platform.d200.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import com.faniot.platform.d200.dto.D200ConfigTaskRequest;
import com.faniot.platform.d200.dto.D200FieldMappingRequest;
import com.faniot.platform.device.domain.Device;
import com.faniot.platform.device.repository.DeviceRepository;
import com.faniot.platform.gateway.domain.Gateway;
import com.faniot.platform.gateway.repository.GatewayRepository;
import com.faniot.platform.iot.dto.TelemetryPointRequest;
import com.faniot.platform.iot.service.IotIngestionService;
import com.faniot.platform.location.dto.LbsLocationRequest;
import com.faniot.platform.location.service.LbsLocationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class D200AdapterService {

    private static final Set<String> STANDARD_FIELDS = Set.of(
            "rpm", "current", "voltage", "power", "frequency", "pressure", "airflow",
            "motorTemperature", "bearingTemperature", "vibration", "status", "alarmCode"
    );

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final GatewayRepository gatewayRepository;
    private final DeviceRepository deviceRepository;
    private final IotIngestionService iotIngestionService;
    private final LbsLocationService lbsLocationService;

    public D200AdapterService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            GatewayRepository gatewayRepository,
            DeviceRepository deviceRepository,
            IotIngestionService iotIngestionService,
            LbsLocationService lbsLocationService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.gatewayRepository = gatewayRepository;
        this.deviceRepository = deviceRepository;
        this.iotIngestionService = iotIngestionService;
        this.lbsLocationService = lbsLocationService;
    }

    @Transactional
    public void ingestMqtt(String topic, int qos, String payload) {
        OffsetDateTime receivedAt = OffsetDateTime.now();
        JsonNode root = readJson(payload);
        String gatewaySn = extractGatewaySn(topic);
        String imei = text(root, "imei");
        String iccid = text(root, "iccid");
        OffsetDateTime deviceTime = parseDeviceTime(root.get("time"));
        JsonNode dataNode = root.get("data");

        Optional<Gateway> gatewayOptional = findGateway(gatewaySn, imei);
        String gatewayId = gatewayOptional.map(Gateway::getGatewayId).orElse(null);
        Long rawId = saveRaw(gatewayId, gatewaySn, topic, qos, imei, iccid, deviceTime, receivedAt, root, dataNode);

        if (gatewayOptional.isEmpty()) {
            markRaw(rawId, false, "未找到匹配的D200盒子，请先按SN或IMEI添加盒子");
            return;
        }
        Gateway gateway = gatewayOptional.get();
        updateGatewayFromPayload(gateway, iccid, receivedAt);
        boolean locationProcessed = processLbs(root, gatewaySn, imei);

        if (dataNode == null || !dataNode.isObject()) {
            markRaw(rawId, locationProcessed, locationProcessed ? null : "D200报文缺少data对象，仅保存原始数据");
            return;
        }

        int saved = processData(gateway, dataNode, deviceTime == null ? receivedAt : deviceTime, receivedAt);
        boolean processed = saved > 0 || locationProcessed;
        markRaw(rawId, processed, processed ? null : "未找到可映射的标准字段或字段映射");
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> rawPayloads(String gatewaySn, Boolean processed, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1");
        if (StringUtils.hasText(gatewaySn)) {
            where.append(" and gateway_sn = ?");
            args.add(gatewaySn);
        }
        if (processed != null) {
            where.append(" and processed = ?");
            args.add(processed);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from d200_raw_payloads" + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from d200_raw_payloads" + where + " order by received_at desc limit ? offset ?",
                this::mapRow,
                args.toArray()
        );
        long safeTotal = total == null ? 0 : total;
        return new PageResponse<>(rows, safePage, safeSize, safeTotal, (int) Math.ceil((double) safeTotal / safeSize));
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> mappings(String gatewayId, String deviceId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1");
        if (StringUtils.hasText(gatewayId)) {
            where.append(" and gateway_id = ?");
            args.add(gatewayId);
        }
        if (StringUtils.hasText(deviceId)) {
            where.append(" and device_id = ?");
            args.add(deviceId);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from d200_field_mappings" + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from d200_field_mappings" + where + " order by id desc limit ? offset ?",
                this::mapRow,
                args.toArray()
        );
        long safeTotal = total == null ? 0 : total;
        return new PageResponse<>(rows, safePage, safeSize, safeTotal, (int) Math.ceil((double) safeTotal / safeSize));
    }

    @Transactional
    public Map<String, Object> createMapping(D200FieldMappingRequest request) {
        jdbcTemplate.update("""
                        insert into d200_field_mappings (
                            gateway_id, device_id, source_key, target_field, scale_factor,
                            offset_value, unit, enabled
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                request.gatewayId(), request.deviceId(), request.sourceKey(), request.targetField(),
                defaultDecimal(request.scaleFactor(), BigDecimal.ONE),
                defaultDecimal(request.offsetValue(), BigDecimal.ZERO),
                request.unit(), request.enabled() == null || request.enabled()
        );
        return jdbcTemplate.queryForObject(
                "select * from d200_field_mappings where gateway_id=? and device_id=? and source_key=? and target_field=? order by id desc limit 1",
                this::mapRow,
                request.gatewayId(), request.deviceId(), request.sourceKey(), request.targetField()
        );
    }

    @Transactional
    public Map<String, Object> updateMapping(Long id, D200FieldMappingRequest request) {
        jdbcTemplate.update("""
                        update d200_field_mappings
                        set gateway_id=?, device_id=?, source_key=?, target_field=?, scale_factor=?,
                            offset_value=?, unit=?, enabled=?, updated_at=current_timestamp
                        where id=?
                        """,
                request.gatewayId(), request.deviceId(), request.sourceKey(), request.targetField(),
                defaultDecimal(request.scaleFactor(), BigDecimal.ONE),
                defaultDecimal(request.offsetValue(), BigDecimal.ZERO),
                request.unit(), request.enabled() == null || request.enabled(), id
        );
        return jdbcTemplate.queryForObject("select * from d200_field_mappings where id=?", this::mapRow, id);
    }

    @Transactional
    public void deleteMapping(Long id) {
        jdbcTemplate.update("delete from d200_field_mappings where id=?", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<Map<String, Object>> configTasks(String gatewaySn, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" where 1=1");
        if (StringUtils.hasText(gatewaySn)) {
            where.append(" and gateway_sn = ?");
            args.add(gatewaySn);
        }
        if (StringUtils.hasText(status)) {
            where.append(" and status = ?");
            args.add(status);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from d200_config_tasks" + where, Long.class, args.toArray());
        args.add(safeSize);
        args.add((safePage - 1) * safeSize);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "select * from d200_config_tasks" + where + " order by id desc limit ? offset ?",
                this::mapRow,
                args.toArray()
        );
        long safeTotal = total == null ? 0 : total;
        return new PageResponse<>(rows, safePage, safeSize, safeTotal, (int) Math.ceil((double) safeTotal / safeSize));
    }

    @Transactional
    public Map<String, Object> createConfigTask(D200ConfigTaskRequest request) {
        jdbcTemplate.update("""
                        insert into d200_config_tasks (gateway_id, gateway_sn, task_type, config_payload, status)
                        values (?, ?, ?, cast(? as jsonb), 'pending')
                        """,
                request.gatewayId(), request.gatewaySn(), request.taskType(), toJson(request.configPayload())
        );
        return jdbcTemplate.queryForObject("select * from d200_config_tasks order by id desc limit 1", this::mapRow);
    }

    @Transactional
    public void markConfigTask(Long id, String status, String errorMessage) {
        jdbcTemplate.update("""
                        update d200_config_tasks
                        set status=?,
                            sent_at=case when ?='sent' then current_timestamp else sent_at end,
                            completed_at=case when ? in ('success','failed','timeout') then current_timestamp else completed_at end,
                            error_message=?
                        where id=?
                        """, status, status, status, errorMessage, id);
    }

    private int processData(Gateway gateway, JsonNode dataNode, OffsetDateTime timestamp, OffsetDateTime receivedAt) {
        Map<String, Mapping> mappingByKey = loadMappings(gateway.getGatewayId());
        Map<String, StandardPayload> payloads = new LinkedHashMap<>();
        Set<String> mappedKeys = new HashSet<>();

        dataNode.fields().forEachRemaining(entry -> {
            String sourceKey = entry.getKey();
            JsonNode value = entry.getValue();
            Mapping mapping = mappingByKey.get(sourceKey);
            if (mapping != null) {
                apply(payloads.computeIfAbsent(mapping.deviceId(), StandardPayload::new), mapping.targetField(), convert(value, mapping), sourceKey);
                mappedKeys.add(sourceKey);
            }
        });

        if (payloads.isEmpty() && hasStandardFields(dataNode)) {
            deviceRepository.findByGatewayIdOrderByIdAsc(gateway.getGatewayId()).stream().findFirst()
                    .ifPresent(device -> payloads.put(device.getDeviceId(), standardPayload(device.getDeviceId(), dataNode)));
        }

        int saved = 0;
        for (StandardPayload payload : payloads.values()) {
            if (!payload.hasAnyStandardValue()) {
                continue;
            }
            iotIngestionService.ingestStandardPoint(gateway, payload.toRequest(timestamp), receivedAt);
            saved++;
        }
        return saved;
    }

    private Map<String, Mapping> loadMappings(String gatewayId) {
        List<Mapping> rows = jdbcTemplate.query("""
                        select gateway_id, device_id, source_key, target_field, scale_factor, offset_value
                        from d200_field_mappings
                        where gateway_id=? and enabled=true
                        """,
                (rs, rowNum) -> new Mapping(
                        rs.getString("device_id"),
                        rs.getString("source_key"),
                        rs.getString("target_field"),
                        rs.getBigDecimal("scale_factor"),
                        rs.getBigDecimal("offset_value")
                ),
                gatewayId
        );
        Map<String, Mapping> result = new HashMap<>();
        for (Mapping row : rows) {
            result.put(row.sourceKey(), row);
        }
        return result;
    }

    private StandardPayload standardPayload(String deviceId, JsonNode dataNode) {
        StandardPayload payload = new StandardPayload(deviceId);
        dataNode.fields().forEachRemaining(entry -> {
            if (STANDARD_FIELDS.contains(entry.getKey())) {
                apply(payload, entry.getKey(), directValue(entry.getValue()), entry.getKey());
            }
        });
        return payload;
    }

    private boolean hasStandardFields(JsonNode dataNode) {
        for (String field : STANDARD_FIELDS) {
            if (dataNode.has(field)) {
                return true;
            }
        }
        return false;
    }

    private void apply(StandardPayload payload, String targetField, Object value, String sourceKey) {
        if (value == null) {
            return;
        }
        switch (targetField) {
            case "rpm" -> payload.rpm = asDecimal(value);
            case "current" -> payload.current = asDecimal(value);
            case "voltage" -> payload.voltage = asDecimal(value);
            case "power" -> payload.power = asDecimal(value);
            case "frequency" -> payload.frequency = asDecimal(value);
            case "pressure" -> payload.pressure = asDecimal(value);
            case "airflow" -> payload.airflow = asDecimal(value);
            case "motorTemperature" -> payload.motorTemperature = asDecimal(value);
            case "bearingTemperature" -> payload.bearingTemperature = asDecimal(value);
            case "vibration" -> payload.vibration = asDecimal(value);
            case "status" -> payload.status = String.valueOf(value);
            case "alarmCode" -> payload.alarmCode = String.valueOf(value);
            default -> {
                // Unknown target fields are intentionally ignored. Raw payload remains available.
            }
        }
    }

    private Object convert(JsonNode value, Mapping mapping) {
        Object direct = directValue(value);
        if (!(direct instanceof BigDecimal decimal)) {
            return direct;
        }
        return decimal.multiply(mapping.scaleFactor()).add(mapping.offsetValue());
    }

    private Object directValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual()) {
            String text = value.asText();
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return text;
            }
        }
        return value.toString();
    }

    private BigDecimal asDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JsonNode readJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("D200原始JSON格式不正确");
        }
    }

    private Long saveRaw(String gatewayId, String gatewaySn, String topic, int qos, String imei, String iccid,
                         OffsetDateTime deviceTime, OffsetDateTime receivedAt, JsonNode rawPayload, JsonNode dataPayload) {
        return jdbcTemplate.queryForObject("""
                        insert into d200_raw_payloads (
                            gateway_id, gateway_sn, topic, qos, imei, iccid, device_time,
                            received_at, raw_payload, data_payload
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb))
                        returning id
                        """,
                Long.class,
                gatewayId, gatewaySn, topic, qos, imei, iccid, deviceTime, receivedAt, toJson(rawPayload),
                dataPayload == null ? null : toJson(dataPayload)
        );
    }

    private void markRaw(Long id, boolean processed, String error) {
        jdbcTemplate.update("update d200_raw_payloads set processed=?, process_error=? where id=?", processed, error, id);
    }

    private Optional<Gateway> findGateway(String gatewaySn, String imei) {
        if (StringUtils.hasText(gatewaySn)) {
            Optional<Gateway> bySn = gatewayRepository.findByGatewaySn(gatewaySn);
            if (bySn.isPresent()) {
                return bySn;
            }
        }
        if (StringUtils.hasText(imei)) {
            return gatewayRepository.findByImei(imei);
        }
        return Optional.empty();
    }

    private void updateGatewayFromPayload(Gateway gateway, String iccid, OffsetDateTime receivedAt) {
        if (StringUtils.hasText(iccid)) {
            gateway.setIccid(iccid);
        }
        gateway.setOnlineStatus("online");
        gateway.setLastSeenAt(receivedAt);
        gatewayRepository.save(gateway);
    }

    private boolean processLbs(JsonNode root, String gatewaySn, String imei) {
        LbsValues values = parseLbs(root);
        if (values == null) {
            return false;
        }
        try {
            lbsLocationService.locateFromTelemetry(new LbsLocationRequest(
                    gatewaySn,
                    imei,
                    values.mcc(),
                    values.mnc(),
                    values.lac(),
                    values.cid()
            ));
            return true;
        } catch (RuntimeException ignored) {
            // LBS failures are recorded by the location service and must not block telemetry ingestion.
            return false;
        }
    }

    private LbsValues parseLbs(JsonNode root) {
        LbsValues direct = parseLbsNode(root);
        if (direct != null) {
            return direct;
        }
        JsonNode data = root.get("data");
        return data != null && data.isObject() ? parseLbsNode(data) : null;
    }

    private LbsValues parseLbsNode(JsonNode root) {
        JsonNode lbs = root.get("lbs");
        if (lbs != null && lbs.isObject()) {
            Long lac = longValue(lbs.get("lac"));
            Long cid = longValue(firstNode(lbs, "cid", "ci"));
            if (lac != null && cid != null) {
                return new LbsValues(intValue(lbs.get("mcc")), intValue(lbs.get("mnc")), lac, cid);
            }
        }
        if (lbs != null && lbs.isTextual()) {
            LbsValues parsed = parseLbsText(lbs.asText());
            if (parsed != null) {
                return parsed;
            }
        }
        Long lac = longValue(root.get("lac"));
        Long cid = longValue(firstNode(root, "cid", "ci"));
        if (lac != null && cid != null) {
            return new LbsValues(intValue(root.get("mcc")), intValue(root.get("mnc")), lac, cid);
        }
        return parseLbsText(text(root, "lbsResponse"));
    }

    private LbsValues parseLbsText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.trim().replace("+LBS:", "");
        String[] parts = normalized.split(",");
        if (parts.length < 2) {
            return null;
        }
        try {
            long cid = parseLong(parts[0]);
            long lac = parseLong(parts[1]);
            Integer mcc = parts.length > 2 ? Integer.valueOf(parts[2].trim()) : null;
            Integer mnc = parts.length > 3 ? Integer.valueOf(parts[3].trim()) : null;
            return new LbsValues(mcc, mnc, lac, cid);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private JsonNode firstNode(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private Integer intValue(JsonNode node) {
        Long value = longValue(node);
        return value == null ? null : value.intValue();
    }

    private Long longValue(JsonNode node) {
        if (node == null || node.isNull() || !StringUtils.hasText(node.asText())) {
            return null;
        }
        try {
            return parseLong(node.asText());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private long parseLong(String value) {
        String normalized = value.trim();
        return normalized.startsWith("0x") || normalized.startsWith("0X")
                ? Long.parseLong(normalized.substring(2), 16)
                : Long.parseLong(normalized);
    }

    private String extractGatewaySn(String topic) {
        String[] parts = topic == null ? new String[0] : topic.split("/");
        if (parts.length >= 4 && "iot".equals(parts[0]) && "d200".equals(parts[1]) && "up".equals(parts[3])) {
            return parts[2];
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private OffsetDateTime parseDeviceTime(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(value.asLong()), ZoneOffset.UTC);
        }
        String text = value.asText();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text);
        } catch (Exception ignored) {
            LocalDateTime local = LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            return local.atOffset(ZoneOffset.UTC);
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("JSON序列化失败");
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        int count = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= count; i++) {
            Object value = rs.getObject(i);
            if (value instanceof Timestamp timestamp) {
                value = timestamp.toInstant().atOffset(ZoneOffset.UTC);
            }
            map.put(toCamel(rs.getMetaData().getColumnLabel(i)), value);
        }
        return map;
    }

    private String toCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (char c : value.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else if (upper) {
                builder.append(Character.toUpperCase(c));
                upper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private record Mapping(String deviceId, String sourceKey, String targetField, BigDecimal scaleFactor, BigDecimal offsetValue) {
    }

    private record LbsValues(Integer mcc, Integer mnc, Long lac, Long cid) {
    }

    private static class StandardPayload {
        private final String deviceId;
        private BigDecimal rpm;
        private BigDecimal current;
        private BigDecimal voltage;
        private BigDecimal power;
        private BigDecimal frequency;
        private BigDecimal pressure;
        private BigDecimal airflow;
        private BigDecimal motorTemperature;
        private BigDecimal bearingTemperature;
        private BigDecimal vibration;
        private String status;
        private String alarmCode;

        private StandardPayload(String deviceId) {
            this.deviceId = deviceId;
        }

        private boolean hasAnyStandardValue() {
            return rpm != null || current != null || voltage != null || power != null || frequency != null
                    || pressure != null || airflow != null || motorTemperature != null || bearingTemperature != null
                    || vibration != null || StringUtils.hasText(status) || StringUtils.hasText(alarmCode);
        }

        private TelemetryPointRequest toRequest(OffsetDateTime timestamp) {
            return new TelemetryPointRequest(
                    deviceId, timestamp, rpm, current, voltage, power, frequency, pressure, airflow,
                    motorTemperature, bearingTemperature, vibration, status, alarmCode
            );
        }
    }
}
