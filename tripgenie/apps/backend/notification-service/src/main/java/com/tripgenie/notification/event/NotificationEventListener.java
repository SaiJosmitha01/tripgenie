package com.tripgenie.notification.event;

import com.tripgenie.common.event.ItineraryGeneratedEvent;
import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.TripCreatedEvent;
import com.tripgenie.common.event.TripUpdatedEvent;
import com.tripgenie.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "tripgenie.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationEventListener {
    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.TRIP_CREATED, groupId = "${tripgenie.kafka.consumer-group}")
    public void onTripCreated(TripCreatedEvent event) {
        notificationService.notifyTripCreated(event);
    }

    @KafkaListener(topics = KafkaTopics.TRIP_UPDATED, groupId = "${tripgenie.kafka.consumer-group}")
    public void onTripUpdated(TripUpdatedEvent event) {
        notificationService.notifyTripUpdated(event);
    }

    @KafkaListener(topics = KafkaTopics.ITINERARY_GENERATED, groupId = "${tripgenie.kafka.consumer-group}")
    public void onItineraryGenerated(ItineraryGeneratedEvent event) {
        notificationService.notifyItineraryGenerated(event);
    }
}
