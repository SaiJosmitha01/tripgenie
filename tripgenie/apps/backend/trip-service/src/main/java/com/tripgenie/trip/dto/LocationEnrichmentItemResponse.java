package com.tripgenie.trip.dto;

import java.util.UUID;

public record LocationEnrichmentItemResponse(
        UUID itineraryItemId,
        String query,
        boolean enriched,
        LocationMetadataResponse location,
        String message
) {
}
