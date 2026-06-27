package com.tripgenie.trip.config;

import com.tripgenie.common.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    NewTopic tripCreatedTopic() {
        return topic(KafkaTopics.TRIP_CREATED);
    }

    @Bean
    NewTopic tripUpdatedTopic() {
        return topic(KafkaTopics.TRIP_UPDATED);
    }

    @Bean
    NewTopic itineraryGeneratedTopic() {
        return topic(KafkaTopics.ITINERARY_GENERATED);
    }

    @Bean
    NewTopic notificationsTopic() {
        return topic(KafkaTopics.NOTIFICATIONS);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
