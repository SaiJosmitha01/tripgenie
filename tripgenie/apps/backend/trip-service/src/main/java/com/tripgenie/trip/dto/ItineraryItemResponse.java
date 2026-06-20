package com.tripgenie.trip.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

public record ItineraryItemResponse(
        UUID id,
        int position,
        String title,
        String description,
        String location,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal estimatedCost,
        String bookingReference
) {
}
