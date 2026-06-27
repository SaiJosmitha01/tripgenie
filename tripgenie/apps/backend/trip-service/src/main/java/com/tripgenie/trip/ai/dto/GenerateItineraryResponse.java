package com.tripgenie.trip.ai.dto;

import com.tripgenie.trip.dto.TripResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GenerateItineraryResponse(
        UUID generationId,
        String provider,
        String model,
        double confidence,
        List<String> warnings,
        Instant generatedAt,
        TripResponse trip
) {
}
