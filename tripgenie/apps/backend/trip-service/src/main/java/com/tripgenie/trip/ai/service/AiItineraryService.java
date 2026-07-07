package com.tripgenie.trip.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.trip.audit.domain.AuditAction;
import com.tripgenie.trip.audit.domain.AuditEntityType;
import com.tripgenie.trip.audit.service.AuditLogService;
import com.tripgenie.trip.ai.config.AiProperties;
import com.tripgenie.trip.ai.dto.AiGenerationContext;
import com.tripgenie.trip.ai.dto.AiItinerary;
import com.tripgenie.trip.ai.dto.AiProviderResponse;
import com.tripgenie.trip.ai.dto.GenerateItineraryRequest;
import com.tripgenie.trip.ai.dto.GenerateItineraryResponse;
import com.tripgenie.trip.ai.provider.AiProvider;
import com.tripgenie.trip.cache.TripCacheService;
import com.tripgenie.trip.domain.AiItineraryGeneration;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.event.TripEventPublisher;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.AiItineraryGenerationRepository;
import com.tripgenie.trip.repository.TripRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AiItineraryService {
    private final TripRepository tripRepository;
    private final AiItineraryGenerationRepository generationRepository;
    private final AiProvider aiProvider;
    private final AiItineraryResponseValidator responseValidator;
    private final AiItineraryNormalizer normalizer;
    private final TripMapper tripMapper;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final TripEventPublisher tripEventPublisher;
    private final TripCacheService tripCacheService;
    private final MeterRegistry meterRegistry;
    private final AuditLogService auditLogService;

    public AiItineraryService(
            TripRepository tripRepository,
            AiItineraryGenerationRepository generationRepository,
            AiProvider aiProvider,
            AiItineraryResponseValidator responseValidator,
            AiItineraryNormalizer normalizer,
            TripMapper tripMapper,
            ObjectMapper objectMapper,
            AiProperties properties,
            TripEventPublisher tripEventPublisher,
            TripCacheService tripCacheService,
            MeterRegistry meterRegistry,
            AuditLogService auditLogService
    ) {
        this.tripRepository = tripRepository;
        this.generationRepository = generationRepository;
        this.aiProvider = aiProvider;
        this.responseValidator = responseValidator;
        this.normalizer = normalizer;
        this.tripMapper = tripMapper;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.tripEventPublisher = tripEventPublisher;
        this.tripCacheService = tripCacheService;
        this.meterRegistry = meterRegistry;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public GenerateItineraryResponse generate(UUID userId, UUID tripId, GenerateItineraryRequest request) {
        try {
            Timer.Sample sample = Timer.start(meterRegistry);
            Trip trip = findOwnedTrip(userId, tripId);
            if (!trip.getItineraryDays().isEmpty() && !request.overwriteExisting()) {
                throw new BusinessException("ITINERARY_ALREADY_EXISTS",
                        "This trip already has an itinerary; set overwriteExisting=true to replace it",
                        HttpStatus.CONFLICT);
            }

            AiGenerationContext context = context(trip, request);
            AiProviderResponse providerResponse = null;
            AiItinerary itinerary = null;
            for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
                providerResponse = aiProvider.generateItinerary(context);
                try {
                    itinerary = responseValidator.parseAndValidate(providerResponse.rawContent(), context);
                    break;
                } catch (AiItineraryResponseValidator.InvalidAiResponseException exception) {
                    if (attempt == properties.maxAttempts()) {
                        throw new BusinessException("AI_RESPONSE_INVALID",
                                "AI returned an invalid itinerary after " + attempt + " attempts", HttpStatus.BAD_GATEWAY);
                    }
                    context = context.withPreviousInvalidResponse(providerResponse.rawContent());
                }
            }

            trip.replaceItinerary(normalizer.toDays(itinerary));
            if (itinerary.budget() != null && trip.getBudget() == null) {
                trip.setBudget(normalizer.toBudget(itinerary.budget()));
            } else if (itinerary.budget() != null && request.overwriteExisting()) {
                normalizer.applyBudget(trip.getBudget(), itinerary.budget());
            }
            Trip savedTrip = tripRepository.save(trip);

            AiItineraryGeneration generation = generation(providerResponse, itinerary, savedTrip);
            AiItineraryGeneration savedGeneration = generationRepository.save(generation);
            tripEventPublisher.publishItineraryGenerated(savedTrip, savedGeneration);
            tripCacheService.evictTrip(userId, tripId);
            GenerateItineraryResponse response = new GenerateItineraryResponse(
                    savedGeneration.getId(), savedGeneration.getProvider(), savedGeneration.getModel(),
                    savedGeneration.getConfidence(), itinerary.quality().warnings(), savedGeneration.getGeneratedAt(),
                    tripMapper.toResponse(savedTrip));
            sample.stop(Timer.builder("tripgenie.ai.itinerary.generation")
                    .tag("provider", savedGeneration.getProvider())
                    .tag("model", savedGeneration.getModel())
                    .register(meterRegistry));
            auditLogService.recordSuccess(userId, AuditAction.ITINERARY_GENERATED, AuditEntityType.AI_GENERATION,
                    savedGeneration.getId(), Map.of(
                            "tripId", tripId,
                            "provider", savedGeneration.getProvider(),
                            "model", savedGeneration.getModel()));
            return response;
        } catch (RuntimeException exception) {
            auditLogService.recordFailure(userId, AuditAction.ITINERARY_GENERATED, AuditEntityType.TRIP,
                    tripId, failureMetadata(exception));
            throw exception;
        }
    }

    private Trip findOwnedTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.getOwnerId().equals(userId)) {
            throw new BusinessException("TRIP_ACCESS_DENIED", "You do not own this trip", HttpStatus.FORBIDDEN);
        }
        return trip;
    }

    private AiGenerationContext context(Trip trip, GenerateItineraryRequest request) {
        BigDecimal budget = request.budget() != null ? request.budget()
                : trip.getBudget() == null ? null : trip.getBudget().getTotalAmount();
        String currency = request.currency() != null ? request.currency().toUpperCase(Locale.ROOT)
                : trip.getBudget() == null ? "USD" : trip.getBudget().getCurrency();
        return new AiGenerationContext(
                trip.getDestination(), trip.getStartDate(), trip.getEndDate(), budget, currency,
                request.travelers(), request.travelStyle().trim(), request.interests().stream().map(String::trim).toList(),
                trimToNull(request.userTravelPreferences()), null);
    }

    private AiItineraryGeneration generation(AiProviderResponse response, AiItinerary itinerary, Trip trip) {
        AiItineraryGeneration generation = new AiItineraryGeneration();
        generation.setTrip(trip);
        generation.setProvider(response.provider());
        generation.setModel(response.model());
        generation.setRawResponse(response.rawContent());
        generation.setConfidence(itinerary.quality().confidence());
        try {
            generation.setQualityWarnings(objectMapper.writeValueAsString(itinerary.quality().warnings()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize AI quality metadata", exception);
        }
        generation.setGeneratedAt(Instant.now());
        return generation;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<String, ?> failureMetadata(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return Map.of("errorCode", businessException.getCode(), "message", message(exception));
        }
        return Map.of("error", exception.getClass().getSimpleName(), "message", message(exception));
    }

    private String message(RuntimeException exception) {
        return exception.getMessage() == null ? "" : exception.getMessage();
    }
}
