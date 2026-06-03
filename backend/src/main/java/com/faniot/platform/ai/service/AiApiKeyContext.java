package com.faniot.platform.ai.service;

public record AiApiKeyContext(
        Long apiKeyId,
        String keyPrefix
) {
}
