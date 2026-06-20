package com.tripgenie.trip.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryDayResponse(
        UUID id,
        int dayNumber,
        LocalDate date,
        String title,
        String notes,
        List<ItineraryItemResponse> items
) {
}
