package com.tripgenie.common.dto;

import java.math.BigDecimal;
import java.util.Set;

public record TravelPreferencesResponse(
        String homeAirport,
        String preferredCurrency,
        Integer defaultTripLengthDays,
        BigDecimal dailyBudget,
        Set<String> travelStyles,
        Set<String> dietaryRestrictions,
        Set<String> accessibilityNeeds
) {
}
