package com.tripgenie.trip.cache;

import com.tripgenie.trip.domain.TripStatus;
import com.tripgenie.trip.dto.PageResponse;
import com.tripgenie.trip.dto.TripResponse;
import com.tripgenie.trip.dto.TripSummaryResponse;
import com.tripgenie.trip.location.dto.PlaceResolution;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class TripCacheService {
    private final CacheManager cacheManager;

    public TripCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public TripResponse getTrip(UUID userId, UUID tripId, Supplier<TripResponse> loader) {
        return getOrLoad(TripCacheNames.TRIP_BY_ID, TripCacheKeys.tripById(userId, tripId), TripResponse.class, loader);
    }

    public PageResponse<TripSummaryResponse> listTrips(
            UUID userId,
            TripStatus status,
            String destination,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            Pageable pageable,
            Supplier<PageResponse<TripSummaryResponse>> loader
    ) {
        String key = TripCacheKeys.tripList(userId, status, destination, startDateFrom, startDateTo, pageable);
        return getOrLoad(TripCacheNames.TRIP_LISTS, key, PageResponse.class, loader);
    }

    public Optional<PlaceResolution> resolvePlace(String query, Supplier<Optional<PlaceResolution>> loader) {
        Cache cache = cache(TripCacheNames.PLACE_RESOLUTIONS);
        String key = TripCacheKeys.placeResolution(query);
        PlaceResolution cached = cache.get(key, PlaceResolution.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<PlaceResolution> loaded = loader.get();
        loaded.ifPresent(resolution -> cache.put(key, resolution));
        return loaded;
    }

    public void evictTrip(UUID userId, UUID tripId) {
        cache(TripCacheNames.TRIP_BY_ID).evict(TripCacheKeys.tripById(userId, tripId));
        evictTripLists();
    }

    public void evictTripLists() {
        cache(TripCacheNames.TRIP_LISTS).clear();
    }

    private <T> T getOrLoad(String cacheName, String key, Class<?> type, Supplier<T> loader) {
        Cache cache = cache(cacheName);
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }
        T loaded = loader.get();
        cache.put(key, loaded);
        return loaded;
    }

    private Cache cache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache is not configured: " + cacheName);
        }
        return cache;
    }
}
