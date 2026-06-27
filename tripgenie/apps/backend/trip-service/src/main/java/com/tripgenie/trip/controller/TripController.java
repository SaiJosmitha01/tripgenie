package com.tripgenie.trip.controller;

import com.tripgenie.common.api.ApiResponse;
import com.tripgenie.trip.ai.dto.GenerateItineraryRequest;
import com.tripgenie.trip.ai.dto.GenerateItineraryResponse;
import com.tripgenie.trip.ai.service.AiItineraryService;
import com.tripgenie.trip.domain.TripStatus;
import com.tripgenie.trip.dto.CreateTripRequest;
import com.tripgenie.trip.dto.PageResponse;
import com.tripgenie.trip.dto.TripResponse;
import com.tripgenie.trip.dto.TripSummaryResponse;
import com.tripgenie.trip.dto.UpdateBudgetRequest;
import com.tripgenie.trip.dto.UpdateItineraryRequest;
import com.tripgenie.trip.dto.UpdateTripRequest;
import com.tripgenie.trip.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
@Tag(name = "Trips")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@Validated
public class TripController {
    private final TripService tripService;
    private final AiItineraryService aiItineraryService;

    public TripController(TripService tripService, AiItineraryService aiItineraryService) {
        this.tripService = tripService;
        this.aiItineraryService = aiItineraryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a trip")
    ApiResponse<TripResponse> create(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody CreateTripRequest request
    ) {
        return ApiResponse.success("Trip created", tripService.createTrip(currentUserId(authentication), request));
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's trips")
    ApiResponse<PageResponse<TripSummaryResponse>> list(
            JwtAuthenticationToken authentication,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) LocalDate startDateFrom,
            @RequestParam(required = false) LocalDate startDateTo,
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success("Trips retrieved", tripService.listTrips(
                currentUserId(authentication), status, destination, startDateFrom, startDateTo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trip by id")
    ApiResponse<TripResponse> get(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        return ApiResponse.success("Trip retrieved", tripService.getTrip(currentUserId(authentication), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update trip details and status")
    ApiResponse<TripResponse> update(
            JwtAuthenticationToken authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRequest request
    ) {
        return ApiResponse.success("Trip updated", tripService.updateTrip(currentUserId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a trip")
    ApiResponse<Void> delete(JwtAuthenticationToken authentication, @PathVariable UUID id) {
        tripService.deleteTrip(currentUserId(authentication), id);
        return ApiResponse.success("Trip deleted", null);
    }

    @PutMapping("/{id}/itinerary")
    @Operation(summary = "Replace a trip's itinerary days and items")
    ApiResponse<TripResponse> updateItinerary(
            JwtAuthenticationToken authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItineraryRequest request
    ) {
        return ApiResponse.success("Itinerary updated",
                tripService.updateItinerary(currentUserId(authentication), id, request));
    }

    @PutMapping("/{id}/budget")
    @Operation(summary = "Create or replace a trip budget and category breakdown")
    ApiResponse<TripResponse> updateBudget(
            JwtAuthenticationToken authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request
    ) {
        return ApiResponse.success("Budget updated",
                tripService.updateBudget(currentUserId(authentication), id, request));
    }

    @PostMapping("/{id}/generate-itinerary")
    @Operation(
            summary = "Generate and save an AI itinerary",
            description = "Uses the configured AI provider. Existing itinerary content is protected unless "
                    + "overwriteExisting is explicitly true. Only the authenticated trip owner can generate."
    )
    ApiResponse<GenerateItineraryResponse> generateItinerary(
            JwtAuthenticationToken authentication,
            @PathVariable UUID id,
            @Valid @RequestBody GenerateItineraryRequest request
    ) {
        return ApiResponse.success("Itinerary generated",
                aiItineraryService.generate(currentUserId(authentication), id, request));
    }

    private UUID currentUserId(JwtAuthenticationToken authentication) {
        return UUID.fromString(authentication.getName());
    }
}
