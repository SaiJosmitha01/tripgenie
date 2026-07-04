package com.tripgenie.trip.location.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.location.config.MapsProperties;
import com.tripgenie.trip.location.dto.PlaceResolution;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class GoogleMapsProvider implements MapsProvider {
    private static final String PROVIDER = "google";
    private static final String OK = "OK";
    private static final String ZERO_RESULTS = "ZERO_RESULTS";

    private final RestClient restClient;
    private final MapsProperties properties;

    public GoogleMapsProvider(RestClient restClient, MapsProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public Optional<PlaceResolution> resolvePlace(String query) {
        if (!properties.hasApiKey()) {
            return Optional.empty();
        }
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        try {
            GoogleTextSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/maps/api/place/textsearch/json")
                            .queryParam("query", query.trim())
                            .queryParam("key", properties.apiKey())
                            .build())
                    .retrieve()
                    .body(GoogleTextSearchResponse.class);
            return toPlaceResolution(response);
        } catch (RestClientException exception) {
            throw new BusinessException("MAPS_PROVIDER_FAILURE",
                    "Google Maps place resolution failed", HttpStatus.BAD_GATEWAY);
        }
    }

    private Optional<PlaceResolution> toPlaceResolution(GoogleTextSearchResponse response) {
        if (response == null || ZERO_RESULTS.equals(response.status())) {
            return Optional.empty();
        }
        if (!OK.equals(response.status())) {
            throw new BusinessException("MAPS_PROVIDER_FAILURE",
                    "Google Maps returned status " + response.status(), HttpStatus.BAD_GATEWAY);
        }
        if (response.results() == null || response.results().isEmpty()) {
            return Optional.empty();
        }
        GooglePlaceResult first = response.results().getFirst();
        Geometry geometry = first.geometry();
        LatLng location = geometry == null ? null : geometry.location();
        return Optional.of(new PlaceResolution(
                first.name(),
                first.formattedAddress(),
                location == null ? null : location.lat(),
                location == null ? null : location.lng(),
                first.placeId(),
                first.rating()
        ));
    }

    record GoogleTextSearchResponse(String status, List<GooglePlaceResult> results) {
    }

    record GooglePlaceResult(
            String name,
            @JsonProperty("formatted_address") String formattedAddress,
            Geometry geometry,
            @JsonProperty("place_id") String placeId,
            BigDecimal rating
    ) {
    }

    record Geometry(LatLng location) {
    }

    record LatLng(BigDecimal lat, BigDecimal lng) {
    }
}
