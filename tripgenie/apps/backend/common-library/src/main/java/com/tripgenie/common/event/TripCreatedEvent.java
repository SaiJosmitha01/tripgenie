package com.tripgenie.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TripCreatedEvent(
        UUID eventId,
        UUID tripId,
        UUID ownerId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant occurredAt
) {
}
