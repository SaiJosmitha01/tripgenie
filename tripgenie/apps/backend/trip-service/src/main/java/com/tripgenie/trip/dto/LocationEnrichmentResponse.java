package com.tripgenie.trip.dto;

import java.util.List;
import java.util.UUID;

public record LocationEnrichmentResponse(
        UUID tripId,
        int totalItems,
        int attemptedItems,
        int enrichedItems,
        String provider,
        List<LocationEnrichmentItemResponse> items
) {
}
