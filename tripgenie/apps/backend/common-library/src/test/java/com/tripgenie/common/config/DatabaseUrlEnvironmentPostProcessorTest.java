package com.tripgenie.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void mapsRenderDatabaseUrlToJdbcDatasourceProperties() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgres://trip_user:secret%21@db.example.com:5432/tripgenie?sslmode=require"
        )));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:5432/tripgenie?sslmode=require");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("trip_user");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret!");
    }

    @Test
    void keepsExplicitDatasourceUrlWhenProvided() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                "DATABASE_URL", "postgres://trip_user:secret@db.example.com:5432/tripgenie",
                "spring.datasource.url", "jdbc:postgresql://explicit.example.com:5432/tripgenie"
        )));

        new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://explicit.example.com:5432/tripgenie");
    }
}
