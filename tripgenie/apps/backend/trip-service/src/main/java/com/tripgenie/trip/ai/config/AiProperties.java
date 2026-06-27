package com.tripgenie.trip.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tripgenie.ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        String model,
        int maxAttempts,
        Duration connectTimeout,
        Duration readTimeout
) {
    public AiProperties {
        baseUrl = valueOrDefault(baseUrl, "https://api.groq.com/openai/v1");
        model = valueOrDefault(model, "llama-3.3-70b-versatile");
        maxAttempts = maxAttempts < 1 ? 2 : maxAttempts;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
