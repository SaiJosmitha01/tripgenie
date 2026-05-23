package com.tripgenie.common.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UUID userId,
        String email,
        Set<String> roles
) {
}
