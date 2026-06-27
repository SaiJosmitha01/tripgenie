package com.tripgenie.common.event;

import java.time.Instant;
import java.util.UUID;

public record ItineraryGeneratedEvent(
        UUID eventId,
        UUID tripId,
        UUID ownerId,
        UUID generationId,
        String destination,
        String provider,
        String model,
        int itineraryDayCount,
        Instant occurredAt
) {
}
