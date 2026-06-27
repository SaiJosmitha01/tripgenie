package com.tripgenie.common.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record TravelPreferencesRequest(
        @Size(max = 50) String homeAirport,
        @Size(max = 50) String preferredCurrency,
        @Min(0) @Max(365) Integer defaultTripLengthDays,
        BigDecimal dailyBudget,
        Set<@Size(max = 50) String> travelStyles,
        Set<@Size(max = 50) String> dietaryRestrictions,
        Set<@Size(max = 50) String> accessibilityNeeds
) {
}
