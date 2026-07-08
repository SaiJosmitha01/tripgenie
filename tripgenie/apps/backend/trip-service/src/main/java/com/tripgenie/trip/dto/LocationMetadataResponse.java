package com.tripgenie.trip.dto;

import java.math.BigDecimal;

public record LocationMetadataResponse(
        String placeName,
        String formattedAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String googlePlaceId,
        BigDecimal rating
) {
}
