package com.tripgenie.common.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String timezone,
        Set<String> roles,
        TravelPreferencesResponse preferences,
        Instant createdAt,
        Instant updatedAt
) {
}
