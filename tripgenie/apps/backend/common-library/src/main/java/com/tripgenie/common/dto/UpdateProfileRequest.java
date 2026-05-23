package com.tripgenie.common.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phoneNumber,
        @Size(max = 50) String timezone
) {
}
