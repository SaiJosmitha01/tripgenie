package com.tripgenie.trip.location.dto;

import java.math.BigDecimal;

public record PlaceResolution(
        String placeName,
        String formattedAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        String googlePlaceId,
        BigDecimal rating
) {
}
