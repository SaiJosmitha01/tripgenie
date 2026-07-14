package com.tripgenie.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "tripgenieDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(databaseUrl)
                || StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_URL"))
                || StringUtils.hasText(environment.getProperty("spring.datasource.url"))) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        if (!"postgres".equals(uri.getScheme()) && !"postgresql".equals(uri.getScheme())) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl(uri));

        Credentials credentials = credentials(uri);
        if (credentials.username() != null && !StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_USERNAME"))) {
            properties.put("spring.datasource.username", credentials.username());
        }
        if (credentials.password() != null && !StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
            properties.put("spring.datasource.password", credentials.password());
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private String jdbcUrl(URI uri) {
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        StringBuilder builder = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() > 0) {
            builder.append(':').append(uri.getPort());
        }
        builder.append(path);
        if (StringUtils.hasText(uri.getRawQuery())) {
            builder.append('?').append(uri.getRawQuery());
        }
        return builder.toString();
    }

    private Credentials credentials(URI uri) {
        if (!StringUtils.hasText(uri.getRawUserInfo())) {
            return new Credentials(null, null);
        }

        String[] parts = uri.getRawUserInfo().split(":", 2);
        String username = decode(parts[0]);
        String password = parts.length == 2 ? decode(parts[1]) : null;
        return new Credentials(username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record Credentials(String username, String password) {
    }
}
