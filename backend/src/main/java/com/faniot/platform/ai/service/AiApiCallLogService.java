package com.faniot.platform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiApiCallLogService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiApiCallLogService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void log(HttpServletRequest request, AiApiKeyContext keyContext, Object params, String responseFormat, int statusCode, boolean success, String errorMessage, long startedAt) {
        String requestJson = toJson(params);
        int durationMs = Math.toIntExact(Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - startedAt));
        jdbcTemplate.update("""
                        INSERT INTO ai_api_call_logs (
                            api_key_id, key_prefix, endpoint, method, request_params,
                            response_format, status_code, success, error_message,
                            client_ip, user_agent, duration_ms
                        )
                        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS inet), ?, ?)
                        """,
                keyContext == null ? null : keyContext.apiKeyId(),
                keyContext == null ? null : keyContext.keyPrefix(),
                request.getRequestURI(),
                request.getMethod(),
                requestJson,
                responseFormat,
                statusCode,
                success,
                errorMessage,
                clientIp(request),
                request.getHeader("User-Agent"),
                durationMs
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object params) {
        try {
            return objectMapper.writeValueAsString(params == null ? java.util.Map.of() : params);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
