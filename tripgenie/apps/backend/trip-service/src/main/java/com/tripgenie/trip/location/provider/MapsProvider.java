package com.tripgenie.trip.location.provider;

import com.tripgenie.trip.location.dto.PlaceResolution;

import java.util.Optional;

public interface MapsProvider {
    String providerName();

    Optional<PlaceResolution> resolvePlace(String query);
}
