package com.tripgenie.trip.location.service;

import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.trip.cache.TripCacheService;
import com.tripgenie.trip.domain.ItineraryItem;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.dto.LocationEnrichmentItemResponse;
import com.tripgenie.trip.dto.LocationEnrichmentResponse;
import com.tripgenie.trip.dto.LocationMetadataResponse;
import com.tripgenie.trip.location.dto.PlaceResolution;
import com.tripgenie.trip.location.provider.MapsProvider;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.TripRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocationEnrichmentService {
    private final TripRepository tripRepository;
    private final MapsProvider mapsProvider;
    private final TripMapper tripMapper;
    private final TripCacheService tripCacheService;
    private final MeterRegistry meterRegistry;

    public LocationEnrichmentService(TripRepository tripRepository,
                                     MapsProvider mapsProvider,
                                     TripMapper tripMapper,
                                     TripCacheService tripCacheService,
                                     MeterRegistry meterRegistry) {
        this.tripRepository = tripRepository;
        this.mapsProvider = mapsProvider;
        this.tripMapper = tripMapper;
        this.tripCacheService = tripCacheService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public LocationEnrichmentResponse enrichTrip(UUID userId, UUID tripId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Trip trip = findOwnedTrip(userId, tripId);
        List<ItineraryItem> items = trip.getItineraryDays().stream()
                .flatMap(day -> day.getItems().stream())
                .toList();

        List<LocationEnrichmentItemResponse> responses = new ArrayList<>();
        int attempted = 0;
        int enriched = 0;
        for (ItineraryItem item : items) {
            String query = queryFor(item);
            if (query == null) {
                responses.add(new LocationEnrichmentItemResponse(
                        item.getId(), null, false, tripMapper.toLocationMetadataResponse(item),
                        "No place name available"));
                continue;
            }
            attempted++;
            Optional<PlaceResolution> resolution = tripCacheService.resolvePlace(
                    query,
                    () -> mapsProvider.resolvePlace(query)
            );
            if (resolution.isEmpty()) {
                responses.add(new LocationEnrichmentItemResponse(
                        item.getId(), query, false, tripMapper.toLocationMetadataResponse(item),
                        "No matching place found"));
                continue;
            }
            applyResolution(item, resolution.get());
            enriched++;
            responses.add(new LocationEnrichmentItemResponse(
                    item.getId(), query, true, toResponse(resolution.get()), "Location enriched"));
        }

        tripRepository.save(trip);
        tripCacheService.evictTrip(userId, tripId);
        LocationEnrichmentResponse response = new LocationEnrichmentResponse(
                trip.getId(), items.size(), attempted, enriched, mapsProvider.providerName(), responses);
        sample.stop(Timer.builder("tripgenie.maps.enrichment")
                .tag("provider", mapsProvider.providerName())
                .register(meterRegistry));
        return response;
    }

    private Trip findOwnedTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.getOwnerId().equals(userId)) {
            throw new BusinessException("TRIP_ACCESS_DENIED", "You do not own this trip", HttpStatus.FORBIDDEN);
        }
        return trip;
    }

    private String queryFor(ItineraryItem item) {
        if (item.getLocation() != null && !item.getLocation().isBlank()) {
            return item.getLocation().trim();
        }
        if (item.getTitle() != null && !item.getTitle().isBlank()) {
            return item.getTitle().trim();
        }
        return null;
    }

    private void applyResolution(ItineraryItem item, PlaceResolution resolution) {
        item.setLocation(valueOrFallback(resolution.placeName(), item.getLocation()));
        item.setFormattedAddress(resolution.formattedAddress());
        item.setLatitude(resolution.latitude());
        item.setLongitude(resolution.longitude());
        item.setGooglePlaceId(resolution.googlePlaceId());
        item.setPlaceRating(resolution.rating());
    }

    private LocationMetadataResponse toResponse(PlaceResolution resolution) {
        return new LocationMetadataResponse(
                resolution.placeName(),
                resolution.formattedAddress(),
                resolution.latitude(),
                resolution.longitude(),
                resolution.googlePlaceId(),
                resolution.rating()
        );
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
