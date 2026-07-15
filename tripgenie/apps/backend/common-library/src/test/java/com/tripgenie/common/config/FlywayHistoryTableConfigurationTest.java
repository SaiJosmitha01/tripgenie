package com.tripgenie.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayHistoryTableConfigurationTest {

    private static final Map<String, String> EXPECTED_TABLES = new LinkedHashMap<>();

    static {
        EXPECTED_TABLES.put("api-gateway", "flyway_schema_history_api_gateway");
        EXPECTED_TABLES.put("auth-service", "flyway_schema_history_auth");
        EXPECTED_TABLES.put("user-service", "flyway_schema_history_user");
        EXPECTED_TABLES.put("trip-service", "flyway_schema_history_trip");
        EXPECTED_TABLES.put("ai-planner-service", "flyway_schema_history_ai_planner");
        EXPECTED_TABLES.put("notification-service", "flyway_schema_history_notification");
    }

    @Test
    void everyFlywayEnabledServiceUsesItsOwnHistoryTableInAllProfiles() throws IOException {
        Path backendDirectory = findBackendDirectory();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

        for (Map.Entry<String, String> entry : EXPECTED_TABLES.entrySet()) {
            Path resources = backendDirectory.resolve(entry.getKey()).resolve("src/main/resources");
            assertThat(property(loader, resources.resolve("application.yml"), "spring.flyway.table"))
                    .as("%s default Flyway table", entry.getKey())
                    .isEqualTo(entry.getValue());
            assertThat(property(loader, resources.resolve("application.yml"), "spring.flyway.baseline-on-migrate"))
                    .as("%s can initialize its history in the shared schema", entry.getKey())
                    .isEqualTo(true);
            assertThat(property(loader, resources.resolve("application.yml"), "spring.flyway.baseline-version"))
                    .as("%s replays all service migrations after baselining", entry.getKey())
                    .isEqualTo(0);

            for (String profile : List.of("local", "prod")) {
                Object override = property(loader, resources.resolve("application-" + profile + ".yml"),
                        "spring.flyway.table");
                assertThat(override)
                        .as("%s must not override its Flyway table in the %s profile", entry.getKey(), profile)
                        .isIn(null, entry.getValue());
            }
        }

        assertThat(EXPECTED_TABLES.values()).doesNotHaveDuplicates();
    }

    private Object property(YamlPropertySourceLoader loader, Path path, String name) throws IOException {
        List<PropertySource<?>> sources = loader.load(path.getFileName().toString(), new FileSystemResource(path));
        return sources.stream()
                .map(source -> source.getProperty(name))
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private Path findBackendDirectory() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path backend = current.resolve("apps/backend");
            if (Files.isDirectory(backend)) {
                return backend;
            }
            if (current.getFileName() != null && current.getFileName().toString().equals("common-library")) {
                Path siblingBackend = current.getParent();
                if (siblingBackend != null && Files.isDirectory(siblingBackend.resolve("auth-service"))) {
                    return siblingBackend;
                }
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not locate apps/backend");
    }
}
