package com.tripgenie.trip.dto;

import com.tripgenie.trip.domain.TripStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripResponse(
        UUID id,
        UUID ownerId,
        String title,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        TripStatus status,
        String description,
        List<ItineraryDayResponse> itinerary,
        BudgetResponse budget,
        Instant createdAt,
        Instant updatedAt
) {
}
