package com.tripgenie.trip.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record GenerateItineraryRequest(
        @Schema(description = "Replace an existing itinerary. Defaults to false to protect user edits.")
        boolean overwriteExisting,
        @DecimalMin("0.00") BigDecimal budget,
        @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @Min(1) @Max(50) int travelers,
        @NotBlank @Size(max = 100) String travelStyle,
        @NotNull @Size(max = 30) List<@NotBlank @Size(max = 100) String> interests,
        @Size(max = 4000) String userTravelPreferences
) {
}
