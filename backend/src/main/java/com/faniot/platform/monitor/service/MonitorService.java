package com.faniot.platform.monitor.service;

import com.faniot.platform.monitor.vo.RealtimeMetricVO;
import com.faniot.platform.monitor.vo.RealtimeOverviewVO;
import com.faniot.platform.monitor.vo.TelemetryHistoryPointVO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MonitorService {

    private final JdbcTemplate jdbcTemplate;

    public MonitorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public RealtimeOverviewVO overview() {
        long onlineGateways = count("select count(*) from gateways where online_status = 'online'");
        long offlineGateways = count("select count(*) from gateways where online_status = 'offline'");
        long onlineDevices = count("select count(*) from devices where status = 'online'");
        long offlineDevices = count("select count(*) from devices where status = 'offline'");
        long alarmDevices = count("select count(*) from devices where status = 'alarm'");
        long activeAlarms = count("select count(*) from alarms where status = 'active'");
        List<RealtimeMetricVO> latestDevices = latest(null, 10);
        return new RealtimeOverviewVO(onlineGateways, offlineGateways, onlineDevices, offlineDevices, alarmDevices, activeAlarms, latestDevices);
    }

    @Transactional(readOnly = true)
    public List<RealtimeMetricVO> latest(String deviceId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        if (deviceId != null && !deviceId.isBlank()) {
            return jdbcTemplate.query("""
                            SELECT * FROM (
                                SELECT DISTINCT ON (dt.device_id)
                                    dt.gateway_id, dt.device_id, d.device_name, dt.status,
                                    dt.rpm, dt.current, dt.voltage, dt.power, dt.frequency, dt.pressure, dt.airflow,
                                    dt.motor_temperature, dt.bearing_temperature, dt.vibration,
                                    dt.alarm_code, dt.timestamp, dt.received_at
                                FROM device_telemetry dt
                                JOIN devices d ON d.device_id = dt.device_id
                                WHERE dt.device_id = ?
                                ORDER BY dt.device_id, dt.timestamp DESC
                            ) latest
                            ORDER BY timestamp DESC
                            LIMIT ?
                            """, this::mapMetric, deviceId, safeLimit);
        }
        return jdbcTemplate.query("""
                        SELECT * FROM (
                            SELECT DISTINCT ON (dt.device_id)
                                dt.gateway_id, dt.device_id, d.device_name, dt.status,
                                dt.rpm, dt.current, dt.voltage, dt.power, dt.frequency, dt.pressure, dt.airflow,
                                dt.motor_temperature, dt.bearing_temperature, dt.vibration,
                                dt.alarm_code, dt.timestamp, dt.received_at
                            FROM device_telemetry dt
                            JOIN devices d ON d.device_id = dt.device_id
                            ORDER BY dt.device_id, dt.timestamp DESC
                        ) latest
                        ORDER BY timestamp DESC
                        LIMIT ?
                        """, this::mapMetric, safeLimit);
    }

    @Transactional(readOnly = true)
    public List<TelemetryHistoryPointVO> history(String deviceId, OffsetDateTime start, OffsetDateTime end, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        List<TelemetryHistoryPointVO> rows = jdbcTemplate.query("""
                        SELECT timestamp, rpm, current, voltage, power, motor_temperature, bearing_temperature, vibration
                        FROM device_telemetry
                        WHERE device_id = ?
                          AND (?::timestamptz IS NULL OR timestamp >= ?)
                          AND (?::timestamptz IS NULL OR timestamp <= ?)
                        ORDER BY timestamp DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new TelemetryHistoryPointVO(
                        rs.getObject("timestamp", OffsetDateTime.class),
                        rs.getBigDecimal("rpm"),
                        rs.getBigDecimal("current"),
                        rs.getBigDecimal("voltage"),
                        rs.getBigDecimal("power"),
                        rs.getBigDecimal("motor_temperature"),
                        rs.getBigDecimal("bearing_temperature"),
                        rs.getBigDecimal("vibration")
                ),
                deviceId, start, start, end, end, safeLimit
        );
        List<TelemetryHistoryPointVO> ordered = new ArrayList<>(rows);
        Collections.reverse(ordered);
        return ordered;
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private RealtimeMetricVO mapMetric(ResultSet rs, int rowNum) throws SQLException {
        return new RealtimeMetricVO(
                rs.getString("gateway_id"),
                rs.getString("device_id"),
                rs.getString("device_name"),
                rs.getString("status"),
                rs.getBigDecimal("rpm"),
                rs.getBigDecimal("current"),
                rs.getBigDecimal("voltage"),
                rs.getBigDecimal("power"),
                rs.getBigDecimal("frequency"),
                rs.getBigDecimal("pressure"),
                rs.getBigDecimal("airflow"),
                rs.getBigDecimal("motor_temperature"),
                rs.getBigDecimal("bearing_temperature"),
                rs.getBigDecimal("vibration"),
                rs.getString("alarm_code"),
                rs.getObject("timestamp", OffsetDateTime.class),
                rs.getObject("received_at", OffsetDateTime.class)
        );
    }
}
