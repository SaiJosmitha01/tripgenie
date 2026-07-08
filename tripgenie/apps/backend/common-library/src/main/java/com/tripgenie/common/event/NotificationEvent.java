package com.tripgenie.common.event;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        UUID sourceEventId,
        UUID recipientUserId,
        String notificationType,
        String subject,
        String message,
        Instant occurredAt
) {
}
