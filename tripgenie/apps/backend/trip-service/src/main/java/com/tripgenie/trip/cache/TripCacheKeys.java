package com.tripgenie.trip.cache;

import com.tripgenie.trip.domain.TripStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public final class TripCacheKeys {
    private TripCacheKeys() {
    }

    public static String tripById(UUID userId, UUID tripId) {
        return "user:" + userId + ":trip:" + tripId;
    }

    public static String tripList(
            UUID userId,
            TripStatus status,
            String destination,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            Pageable pageable
    ) {
        return "user:%s:status:%s:destination:%s:startFrom:%s:startTo:%s:page:%d:size:%d:sort:%s"
                .formatted(
                        userId,
                        status == null ? "all" : status.name(),
                        normalize(destination),
                        startDateFrom == null ? "none" : startDateFrom,
                        startDateTo == null ? "none" : startDateTo,
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSort().toString()
                );
    }

    public static String placeResolution(String query) {
        return "place:" + normalize(query);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank()
                ? "none"
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
