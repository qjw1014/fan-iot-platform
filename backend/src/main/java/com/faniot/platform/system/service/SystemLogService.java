package com.faniot.platform.system.service;

import com.faniot.platform.common.api.PageResponse;
import com.faniot.platform.system.domain.SystemLog;
import com.faniot.platform.system.repository.SystemLogRepository;
import com.faniot.platform.system.vo.SystemLogVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;
    private final JdbcTemplate jdbcTemplate;

    public SystemLogService(SystemLogRepository systemLogRepository, JdbcTemplate jdbcTemplate) {
        this.systemLogRepository = systemLogRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResponse<SystemLogVO> page(String keyword, String logLevel, String module, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.from(systemLogRepository.findAll(buildSpec(keyword, logLevel, module), pageable).map(SystemLogVO::from));
    }

    @Transactional
    public void record(String module, String operation, String message) {
        jdbcTemplate.update("""
                        INSERT INTO system_logs (log_type, log_level, module, operation, message)
                        VALUES ('operation', 'info', ?, ?, ?)
                        """,
                module, operation, message
        );
    }

    private Specification<SystemLog> buildSpec(String keyword, String logLevel, String module) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(logLevel)) {
                predicates.add(cb.equal(root.get("logLevel"), logLevel));
            }
            if (StringUtils.hasText(module)) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("message"), like),
                        cb.like(root.get("operation"), like),
                        cb.like(root.get("module"), like)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
