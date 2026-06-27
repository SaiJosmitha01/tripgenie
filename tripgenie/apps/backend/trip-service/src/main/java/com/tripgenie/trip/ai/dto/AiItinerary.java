package com.tripgenie.trip.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AiItinerary(
        @NotNull @Size(min = 1) @JsonProperty("itinerary_days") List<@Valid Day> itineraryDays,
        @Valid Budget budget,
        @NotNull @Valid Quality quality
) {
    public record Day(
            @Min(1) @JsonProperty("day_number") int dayNumber,
            @NotNull LocalDate date,
            @Size(max = 160) String title,
            @Size(max = 4000) String notes,
            @NotNull @Size(min = 1) @JsonProperty("itinerary_items") List<@Valid Item> itineraryItems
    ) {
    }

    public record Item(
            @Min(1) int position,
            @NotBlank @Size(max = 180) String title,
            @Size(max = 4000) String description,
            @Size(max = 180) String location,
            @JsonProperty("start_time") LocalTime startTime,
            @JsonProperty("end_time") LocalTime endTime,
            @DecimalMin("0.00") @JsonProperty("estimated_cost") BigDecimal estimatedCost
    ) {
    }

    public record Budget(
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotNull @DecimalMin("0.00") @JsonProperty("total_amount") BigDecimal totalAmount,
            @NotNull List<@Valid Category> categories,
            @Size(max = 4000) String notes
    ) {
    }

    public record Category(
            @NotBlank @Size(max = 80) String name,
            @NotNull @DecimalMin("0.00") BigDecimal amount
    ) {
    }

    public record Quality(
            @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
            @NotNull List<@NotBlank @Size(max = 500) String> warnings,
            @NotBlank @Size(max = 1000) String rationale
    ) {
    }
}
