package com.tripgenie.trip.event;

import com.tripgenie.common.event.ItineraryGeneratedEvent;
import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.TripCreatedEvent;
import com.tripgenie.common.event.TripUpdatedEvent;
import com.tripgenie.trip.domain.AiItineraryGeneration;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.domain.TripStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TripEventPublisherTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TripEventPublisher publisher;
    private Trip trip;

    @BeforeEach
    void setUp() {
        publisher = new TripEventPublisher(kafkaTemplate, true);
        trip = new Trip();
        ReflectionTestUtils.setField(trip, "id", UUID.randomUUID());
        trip.setOwnerId(UUID.randomUUID());
        trip.setTitle("Japan");
        trip.setDestination("Tokyo");
        trip.setStartDate(LocalDate.of(2026, 9, 1));
        trip.setEndDate(LocalDate.of(2026, 9, 7));
        trip.setStatus(TripStatus.DRAFT);
    }

    @Test
    void publishesTripCreatedEventToKafka() {
        publisher.publishTripCreated(trip);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq(KafkaTopics.TRIP_CREATED),
                org.mockito.Mockito.eq(trip.getId().toString()), eventCaptor.capture());
        TripCreatedEvent event = (TripCreatedEvent) eventCaptor.getValue();
        assertThat(event.tripId()).isEqualTo(trip.getId());
        assertThat(event.ownerId()).isEqualTo(trip.getOwnerId());
        assertThat(event.destination()).isEqualTo("Tokyo");
    }

    @Test
    void publishesTripUpdatedEventToKafka() {
        publisher.publishTripUpdated(trip);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq(KafkaTopics.TRIP_UPDATED),
                org.mockito.Mockito.eq(trip.getId().toString()), eventCaptor.capture());
        TripUpdatedEvent event = (TripUpdatedEvent) eventCaptor.getValue();
        assertThat(event.tripId()).isEqualTo(trip.getId());
        assertThat(event.status()).isEqualTo("DRAFT");
    }

    @Test
    void publishesItineraryGeneratedEventToKafka() {
        AiItineraryGeneration generation = new AiItineraryGeneration();
        ReflectionTestUtils.setField(generation, "id", UUID.randomUUID());
        generation.setProvider("groq");
        generation.setModel("test-model");

        publisher.publishItineraryGenerated(trip, generation);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq(KafkaTopics.ITINERARY_GENERATED),
                org.mockito.Mockito.eq(trip.getId().toString()), eventCaptor.capture());
        ItineraryGeneratedEvent event = (ItineraryGeneratedEvent) eventCaptor.getValue();
        assertThat(event.generationId()).isEqualTo(generation.getId());
        assertThat(event.provider()).isEqualTo("groq");
    }

    @Test
    void skipsPublishingWhenKafkaIsDisabled() {
        publisher = new TripEventPublisher(kafkaTemplate, false);

        publisher.publishTripCreated(trip);

        verifyNoInteractions(kafkaTemplate);
    }
}
