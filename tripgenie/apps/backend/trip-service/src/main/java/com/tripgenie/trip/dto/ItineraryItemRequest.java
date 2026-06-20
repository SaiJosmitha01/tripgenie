package com.tripgenie.trip.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ItineraryItemRequest(
        @Min(1) int position,
        @NotBlank @Size(max = 180) String title,
        @Size(max = 4000) String description,
        @Size(max = 180) String location,
        LocalTime startTime,
        LocalTime endTime,
        @DecimalMin("0.00") BigDecimal estimatedCost,
        @Size(max = 160) String bookingReference
) {
}
