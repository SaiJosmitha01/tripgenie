package com.tripgenie.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateItineraryRequest(
        @NotNull List<@Valid ItineraryDayRequest> days
) {
}
