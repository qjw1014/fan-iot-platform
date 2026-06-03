package com.faniot.platform.alarm.service;

import com.faniot.platform.alarm.vo.AlarmVO;
import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AlarmService {

    private final JdbcTemplate jdbcTemplate;

    public AlarmService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<AlarmVO> page(String keyword, String alarmLevel, String status, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String like = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        String sql = """
                FROM alarms
                WHERE (?::varchar IS NULL OR device_id LIKE ? OR alarm_message LIKE ? OR alarm_code LIKE ?)
                  AND (?::varchar IS NULL OR alarm_level = ?)
                  AND (?::varchar IS NULL OR status = ?)
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT count(*) " + sql,
                Long.class,
                like, like, like, like,
                blankToNull(alarmLevel), blankToNull(alarmLevel),
                blankToNull(status), blankToNull(status)
        );
        List<AlarmVO> records = jdbcTemplate.query(
                "SELECT * " + sql + " ORDER BY occurred_at DESC LIMIT ? OFFSET ?",
                this::mapAlarm,
                like, like, like, like,
                blankToNull(alarmLevel), blankToNull(alarmLevel),
                blankToNull(status), blankToNull(status),
                safeSize, (safePage - 1) * safeSize
        );
        long safeTotal = total == null ? 0 : total;
        return new PageResponse<>(records, safePage, safeSize, safeTotal, (int) Math.ceil((double) safeTotal / safeSize));
    }

    @Transactional
    public void acknowledge(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = jdbcTemplate.update("""
                UPDATE alarms
                SET acknowledged = true,
                    acknowledged_at = ?,
                    status = CASE WHEN status = 'active' THEN 'acknowledged' ELSE status END,
                    updated_at = ?
                WHERE id = ?
                """, now, now, id);
        if (updated == 0) {
            throw new BusinessException("告警不存在");
        }
    }

    @Transactional
    public void close(Long id) {
        OffsetDateTime now = OffsetDateTime.now();
        int updated = jdbcTemplate.update("""
                UPDATE alarms
                SET status = 'closed',
                    recovered_at = COALESCE(recovered_at, ?),
                    updated_at = ?
                WHERE id = ?
                """, now, now, id);
        if (updated == 0) {
            throw new BusinessException("告警不存在");
        }
    }

    private AlarmVO mapAlarm(ResultSet rs, int rowNum) throws SQLException {
        return new AlarmVO(
                rs.getLong("id"),
                rs.getString("alarm_id"),
                columnExists(rs, "gateway_id") ? rs.getString("gateway_id") : null,
                rs.getString("device_id"),
                rs.getString("alarm_type"),
                rs.getString("alarm_level"),
                rs.getString("alarm_code"),
                rs.getString("alarm_message"),
                rs.getObject("occurred_at", OffsetDateTime.class),
                rs.getObject("recovered_at", OffsetDateTime.class),
                rs.getBoolean("acknowledged"),
                rs.getObject("acknowledged_at", OffsetDateTime.class),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }

    private boolean columnExists(ResultSet rs, String column) {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
