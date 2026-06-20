package com.tripgenie.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ItineraryDayRequest(
        @Min(1) int dayNumber,
        @NotNull LocalDate date,
        @Size(max = 160) String title,
        @Size(max = 4000) String notes,
        @NotNull List<@Valid ItineraryItemRequest> items
) {
}
