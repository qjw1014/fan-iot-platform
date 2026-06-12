package com.faniot.platform.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.lbs")
public record LbsProperties(
        boolean enabled,
        String provider,
        String baseUrl,
        String apiKey,
        Integer defaultMcc,
        Integer defaultMnc,
        String network,
        int signal,
        int connectTimeoutSeconds,
        int readTimeoutSeconds
) {
    public LbsProperties {
        provider = valueOrDefault(provider, "cellocation");
        baseUrl = valueOrDefault(baseUrl, "http://api.cellocation.com:84/cell/");
        defaultMcc = defaultMcc == null ? 460 : defaultMcc;
        network = valueOrDefault(network, "GSM");
        signal = signal == 0 ? -99 : signal;
        connectTimeoutSeconds = connectTimeoutSeconds <= 0 ? 5 : connectTimeoutSeconds;
        readTimeoutSeconds = readTimeoutSeconds <= 0 ? 8 : readTimeoutSeconds;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
