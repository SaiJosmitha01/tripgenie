package com.tripgenie.trip.location.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tripgenie.maps")
public record MapsProperties(
        String baseUrl,
        String apiKey,
        int retryAttempts,
        Duration connectTimeout,
        Duration readTimeout
) {
    public MapsProperties {
        baseUrl = valueOrDefault(baseUrl, "https://maps.googleapis.com");
        retryAttempts = retryAttempts < 1 ? 2 : retryAttempts;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
