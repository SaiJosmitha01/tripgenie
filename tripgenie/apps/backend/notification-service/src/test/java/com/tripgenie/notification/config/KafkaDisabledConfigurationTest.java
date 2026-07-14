package com.tripgenie.notification.config;

import com.tripgenie.notification.event.NotificationEventListener;
import com.tripgenie.notification.service.EmailService;
import com.tripgenie.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaDisabledConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("tripgenie.kafka.enabled=false")
            .withUserConfiguration(
                    KafkaConsumerConfig.class,
                    NotificationEventListener.class,
                    NotificationService.class,
                    TestDependencies.class
            );

    @Test
    void disablesKafkaListenerInfrastructureButKeepsNotificationServiceAvailable() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(KafkaTemplate.class);
            assertThat(context).doesNotHaveBean(NotificationEventListener.class);
            assertThat(context).hasSingleBean(NotificationService.class);
        });
    }

    @Configuration
    static class TestDependencies {
        @Bean
        EmailService emailService() {
            return mock(EmailService.class);
        }
    }
}
