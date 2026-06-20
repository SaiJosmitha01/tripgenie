package com.tripgenie.trip.dto;

import com.tripgenie.trip.domain.TripStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTripRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 160) String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull TripStatus status,
        @Size(max = 4000) String description
) {
}
