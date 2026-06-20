package com.tripgenie.trip.dto;

import com.tripgenie.trip.domain.TripStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TripSummaryResponse(
        UUID id,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
