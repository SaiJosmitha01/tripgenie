package com.tripgenie.trip.config;

import com.tripgenie.trip.audit.event.NotificationAuditListener;
import com.tripgenie.trip.event.TripEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaDisabledConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("tripgenie.kafka.enabled=false")
            .withUserConfiguration(
                    KafkaConsumerConfig.class,
                    KafkaTopicConfig.class,
                    NotificationAuditListener.class,
                    TripEventPublisher.class
            );

    @Test
    void disablesKafkaInfrastructureButKeepsNoOpPublisherAvailable() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(KafkaTemplate.class);
            assertThat(context).doesNotHaveBean(NewTopic.class);
            assertThat(context).doesNotHaveBean(NotificationAuditListener.class);
            assertThat(context).hasSingleBean(TripEventPublisher.class);
        });
    }
}
