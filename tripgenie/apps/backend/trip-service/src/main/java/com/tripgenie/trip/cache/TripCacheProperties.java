package com.tripgenie.trip.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tripgenie.cache")
public record TripCacheProperties(
        Duration tripTtl,
        Duration tripListTtl,
        Duration placeResolutionTtl
) {
    public TripCacheProperties {
        tripTtl = tripTtl == null ? Duration.ofMinutes(10) : tripTtl;
        tripListTtl = tripListTtl == null ? Duration.ofMinutes(5) : tripListTtl;
        placeResolutionTtl = placeResolutionTtl == null ? Duration.ofDays(7) : placeResolutionTtl;
    }
}
