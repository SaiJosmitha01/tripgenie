package com.tripgenie.notification.event;

import com.tripgenie.common.event.TripUpdatedEvent;
import com.tripgenie.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {
    @Mock
    private NotificationService notificationService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService);
    }

    @Test
    void delegatesTripUpdatedEventsToNotificationService() {
        TripUpdatedEvent event = new TripUpdatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Japan",
                "Kyoto",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                "ACTIVE",
                Instant.now()
        );

        listener.onTripUpdated(event);

        verify(notificationService).notifyTripUpdated(event);
    }
}
