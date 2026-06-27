package com.tripgenie.notification.service;

import com.tripgenie.common.event.ItineraryGeneratedEvent;
import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.NotificationEvent;
import com.tripgenie.common.event.TripCreatedEvent;
import com.tripgenie.common.event.TripUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class NotificationService {
    private final EmailService emailService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public NotificationService(EmailService emailService, KafkaTemplate<String, Object> kafkaTemplate) {
        this.emailService = emailService;
        this.kafkaTemplate = kafkaTemplate;
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
        kafkaTemplate.send(KafkaTopics.NOTIFICATIONS, userId.toString(), notification);
    }
}
