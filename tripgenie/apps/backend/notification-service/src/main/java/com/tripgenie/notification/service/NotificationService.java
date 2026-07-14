package com.tripgenie.notification.service;

import com.tripgenie.common.event.ItineraryGeneratedEvent;
import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.NotificationEvent;
import com.tripgenie.common.event.TripCreatedEvent;
import com.tripgenie.common.event.TripUpdatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {
    private final EmailService emailService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;

    public NotificationService(EmailService emailService,
                               @Nullable KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${tripgenie.kafka.enabled:true}") boolean kafkaEnabled) {
        this.emailService = emailService;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void notifyTripCreated(TripCreatedEvent event) {
        send(event.eventId(), event.ownerId(), "TRIP_CREATED",
                "Trip created: " + event.title(),
                "Your trip to " + event.destination() + " has been created.");
    }

    public void notifyTripUpdated(TripUpdatedEvent event) {
        send(event.eventId(), event.ownerId(), "TRIP_UPDATED",
                "Trip updated: " + event.title(),
                "Your trip to " + event.destination() + " was updated.");
    }

    public void notifyItineraryGenerated(ItineraryGeneratedEvent event) {
        send(event.eventId(), event.ownerId(), "ITINERARY_GENERATED",
                "Itinerary ready for " + event.destination(),
                "Your AI itinerary with " + event.itineraryDayCount() + " day(s) is ready.");
    }

    private void send(UUID sourceEventId, UUID userId, String notificationType, String subject, String message) {
        NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID(), sourceEventId, userId, notificationType, subject, message, Instant.now());
        emailService.sendEmail(userId, subject, message);
        if (kafkaEnabled && kafkaTemplate != null) {
            kafkaTemplate.send(KafkaTopics.NOTIFICATIONS, userId.toString(), notification);
        }
    }
}
