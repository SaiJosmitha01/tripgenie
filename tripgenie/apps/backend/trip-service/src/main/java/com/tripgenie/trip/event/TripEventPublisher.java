package com.tripgenie.trip.event;

import com.tripgenie.common.event.ItineraryGeneratedEvent;
import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.TripCreatedEvent;
import com.tripgenie.common.event.TripUpdatedEvent;
import com.tripgenie.trip.domain.AiItineraryGeneration;
import com.tripgenie.trip.domain.Trip;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TripEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean kafkaEnabled;

    public TripEventPublisher(@Nullable KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${tripgenie.kafka.enabled:true}") boolean kafkaEnabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publishTripCreated(Trip trip) {
        if (!canPublish()) {
            return;
        }
        TripCreatedEvent event = new TripCreatedEvent(
                UUID.randomUUID(),
                trip.getId(),
                trip.getOwnerId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.TRIP_CREATED, trip.getId().toString(), event);
    }

    public void publishTripUpdated(Trip trip) {
        if (!canPublish()) {
            return;
        }
        TripUpdatedEvent event = new TripUpdatedEvent(
                UUID.randomUUID(),
                trip.getId(),
                trip.getOwnerId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus().name(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.TRIP_UPDATED, trip.getId().toString(), event);
    }

    public void publishItineraryGenerated(Trip trip, AiItineraryGeneration generation) {
        if (!canPublish()) {
            return;
        }
        ItineraryGeneratedEvent event = new ItineraryGeneratedEvent(
                UUID.randomUUID(),
                trip.getId(),
                trip.getOwnerId(),
                generation.getId(),
                trip.getDestination(),
                generation.getProvider(),
                generation.getModel(),
                trip.getItineraryDays().size(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.ITINERARY_GENERATED, trip.getId().toString(), event);
    }

    private boolean canPublish() {
        return kafkaEnabled && kafkaTemplate != null;
    }
}
