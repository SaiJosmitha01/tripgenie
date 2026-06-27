package com.tripgenie.notification.service;

import com.tripgenie.common.event.KafkaTopics;
import com.tripgenie.common.event.NotificationEvent;
import com.tripgenie.common.event.TripCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private EmailService emailService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailService, kafkaTemplate);
    }

    @Test
    void sendsEmailAndPublishesNotificationForTripCreated() {
        UUID ownerId = UUID.randomUUID();
        TripCreatedEvent event = new TripCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ownerId,
                "Japan",
                "Tokyo",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "DRAFT",
                Instant.now()
        );

        notificationService.notifyTripCreated(event);

        verify(emailService).sendEmail(org.mockito.Mockito.eq(ownerId),
                org.mockito.Mockito.eq("Trip created: Japan"),
                org.mockito.Mockito.contains("Tokyo"));
        ArgumentCaptor<Object> notificationCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(org.mockito.Mockito.eq(KafkaTopics.NOTIFICATIONS),
                org.mockito.Mockito.eq(ownerId.toString()), notificationCaptor.capture());
        NotificationEvent notification = (NotificationEvent) notificationCaptor.getValue();
        assertThat(notification.sourceEventId()).isEqualTo(event.eventId());
        assertThat(notification.notificationType()).isEqualTo("TRIP_CREATED");
    }
}
