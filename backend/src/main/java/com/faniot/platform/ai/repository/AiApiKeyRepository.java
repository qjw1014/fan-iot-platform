package com.faniot.platform.ai.repository;

import com.faniot.platform.ai.domain.AiApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiApiKeyRepository extends JpaRepository<AiApiKey, Long> {

    Optional<AiApiKey> findByKeyPrefix(String keyPrefix);
}
