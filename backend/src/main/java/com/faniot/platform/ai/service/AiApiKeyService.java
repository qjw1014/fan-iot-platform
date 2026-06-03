package com.faniot.platform.ai.service;

import com.faniot.platform.ai.domain.AiApiKey;
import com.faniot.platform.ai.repository.AiApiKeyRepository;
import com.faniot.platform.common.exception.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Service
public class AiApiKeyService {

    private final AiApiKeyRepository aiApiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public AiApiKeyService(AiApiKeyRepository aiApiKeyRepository, PasswordEncoder passwordEncoder) {
        this.aiApiKeyRepository = aiApiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AiApiKeyContext authenticate(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException("缺少AI API Key");
        }
        String normalizedKey = apiKey.trim();
        String prefix = extractPrefix(normalizedKey);
        AiApiKey key = aiApiKeyRepository.findByKeyPrefix(prefix)
                .orElseThrow(() -> new BusinessException("AI API Key无效"));
        if (!Boolean.TRUE.equals(key.getEnabled())) {
            throw new BusinessException("AI API Key已禁用");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("AI API Key已过期");
        }
        if (!passwordEncoder.matches(normalizedKey, key.getKeyHash())) {
            throw new BusinessException("AI API Key无效");
        }
        key.setLastUsedAt(OffsetDateTime.now());
        aiApiKeyRepository.save(key);
        return new AiApiKeyContext(key.getId(), key.getKeyPrefix());
    }

    private String extractPrefix(String apiKey) {
        int index = apiKey.indexOf('_');
        if (index <= 0) {
            return apiKey.length() <= 32 ? apiKey : apiKey.substring(0, 32);
        }
        return apiKey.substring(0, index);
    }
}
