package com.tripgenie.trip.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AiGenerationContext(
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal budget,
        String currency,
        int travelers,
        String travelStyle,
        List<String> interests,
        String userTravelPreferences,
        String previousInvalidResponse
) {
    public AiGenerationContext withPreviousInvalidResponse(String response) {
        return new AiGenerationContext(destination, startDate, endDate, budget, currency, travelers,
                travelStyle, interests, userTravelPreferences, response);
    }
}
