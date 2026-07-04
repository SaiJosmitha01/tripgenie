package com.tripgenie.trip.location.provider;

import com.tripgenie.trip.location.dto.PlaceResolution;

import java.util.Optional;

public class NoOpMapsProvider implements MapsProvider {
    @Override
    public String providerName() {
        return "noop";
    }

    @Override
    public Optional<PlaceResolution> resolvePlace(String query) {
        return Optional.empty();
    }
}
