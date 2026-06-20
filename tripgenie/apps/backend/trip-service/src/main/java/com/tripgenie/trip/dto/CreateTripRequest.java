package com.tripgenie.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTripRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 160) String destination,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 4000) String description
) {
}
