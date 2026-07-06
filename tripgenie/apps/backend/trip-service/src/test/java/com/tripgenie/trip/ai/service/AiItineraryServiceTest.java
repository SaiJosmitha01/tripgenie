package com.tripgenie.trip.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.ai.config.AiProperties;
import com.tripgenie.trip.ai.dto.AiProviderResponse;
import com.tripgenie.trip.ai.dto.GenerateItineraryRequest;
import com.tripgenie.trip.ai.dto.GenerateItineraryResponse;
import com.tripgenie.trip.ai.provider.AiProvider;
import com.tripgenie.trip.cache.TripCacheNames;
import com.tripgenie.trip.cache.TripCacheService;
import com.tripgenie.trip.domain.AiItineraryGeneration;
import com.tripgenie.trip.domain.ItineraryDay;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.domain.TripStatus;
import com.tripgenie.trip.event.TripEventPublisher;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.AiItineraryGenerationRepository;
import com.tripgenie.trip.repository.TripRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiItineraryServiceTest {
    @Mock
    private TripRepository tripRepository;
    @Mock
    private AiItineraryGenerationRepository generationRepository;
    @Mock
    private AiProvider aiProvider;
    @Mock
    private TripEventPublisher tripEventPublisher;

    private AiItineraryService service;
    private UUID ownerId;
    private UUID tripId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        AiProperties properties = new AiProperties(null, "test-key", null, 2, 2,
                Duration.ofSeconds(1), Duration.ofSeconds(1));
        service = new AiItineraryService(
                tripRepository,
                generationRepository,
                aiProvider,
                new AiItineraryResponseValidator(objectMapper, validator),
                new AiItineraryNormalizer(),
                new TripMapper(),
                objectMapper,
                properties,
                tripEventPublisher,
                cacheService(),
                new SimpleMeterRegistry()
        );
        ownerId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        trip = trip(ownerId, tripId);
    }

    @Test
    void generatesValidItineraryAndStoresRawResponse() {
        String raw = validResponse();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(aiProvider.generateItinerary(any())).thenReturn(new AiProviderResponse("groq", "test-model", raw));
        when(tripRepository.save(trip)).thenReturn(trip);
        when(generationRepository.save(any())).thenAnswer(invocation -> {
            AiItineraryGeneration generation = invocation.getArgument(0);
            ReflectionTestUtils.setField(generation, "id", UUID.randomUUID());
            return generation;
        });

        GenerateItineraryResponse response = service.generate(ownerId, tripId, request(false));

        assertThat(response.provider()).isEqualTo("groq");
        assertThat(response.confidence()).isEqualTo(0.88);
        assertThat(response.trip().itinerary()).hasSize(2);
        assertThat(response.trip().budget().totalAmount()).isEqualByComparingTo("500.00");
        verify(generationRepository).save(org.mockito.ArgumentMatchers.argThat(generation ->
                generation.getRawResponse().equals(raw) && generation.getTrip() == trip));
        verify(tripEventPublisher).publishItineraryGenerated(org.mockito.Mockito.eq(trip), any());
    }

    @Test
    void retriesAfterMalformedJsonAndUsesCorrectedResponse() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(aiProvider.generateItinerary(any()))
                .thenReturn(new AiProviderResponse("groq", "test-model", "not-json"))
                .thenReturn(new AiProviderResponse("groq", "test-model", validResponse()));
        when(tripRepository.save(trip)).thenReturn(trip);
        when(generationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateItineraryResponse response = service.generate(ownerId, tripId, request(false));

        assertThat(response.trip().itinerary()).hasSize(2);
        verify(aiProvider, org.mockito.Mockito.times(2)).generateItinerary(any());
    }

    @Test
    void rejectsResponseAfterConfiguredAttemptsWithoutSaving() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(aiProvider.generateItinerary(any()))
                .thenReturn(new AiProviderResponse("groq", "test-model", "{}"));

        assertThatThrownBy(() -> service.generate(ownerId, tripId, request(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("AI_RESPONSE_INVALID");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });

        verify(aiProvider, org.mockito.Mockito.times(2)).generateItinerary(any());
        verify(tripRepository, never()).save(any());
        verifyNoInteractions(generationRepository);
    }

    @Test
    void protectsExistingItineraryUnlessOverwriteIsExplicit() {
        trip.replaceItinerary(List.of(new ItineraryDay()));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.generate(ownerId, tripId, request(false)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("ITINERARY_ALREADY_EXISTS"));

        verifyNoInteractions(aiProvider);
    }

    @Test
    void rejectsNonOwnerBeforeCallingProvider() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), tripId, request(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("TRIP_ACCESS_DENIED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });

        verifyNoInteractions(aiProvider);
    }

    private GenerateItineraryRequest request(boolean overwrite) {
        return new GenerateItineraryRequest(
                overwrite, new BigDecimal("1000.00"), "USD", 2, "balanced",
                List.of("food", "museums"), "Avoid very early mornings");
    }

    private Trip trip(UUID owner, UUID id) {
        Trip value = new Trip();
        ReflectionTestUtils.setField(value, "id", id);
        value.setOwnerId(owner);
        value.setTitle("Paris");
        value.setDestination("Paris, France");
        value.setStartDate(LocalDate.of(2026, 9, 1));
        value.setEndDate(LocalDate.of(2026, 9, 2));
        value.setStatus(TripStatus.DRAFT);
        return value;
    }

    private String validResponse() {
        return """
                {
                  "itinerary_days": [
                    {
                      "day_number": 1,
                      "date": "2026-09-01",
                      "title": "Arrival",
                      "notes": "Allow time to settle in",
                      "itinerary_items": [{
                        "position": 1,
                        "title": "Neighborhood walk",
                        "description": "Explore nearby streets",
                        "location": "Le Marais",
                        "start_time": "15:00:00",
                        "end_time": "17:00:00",
                        "estimated_cost": 20.00
                      }]
                    },
                    {
                      "day_number": 2,
                      "date": "2026-09-02",
                      "title": "Museums",
                      "notes": null,
                      "itinerary_items": [{
                        "position": 1,
                        "title": "Museum visit",
                        "description": null,
                        "location": "Central Paris",
                        "start_time": "10:00:00",
                        "end_time": "13:00:00",
                        "estimated_cost": 40.00
                      }]
                    }
                  ],
                  "budget": {
                    "currency": "USD",
                    "total_amount": 500.00,
                    "categories": [
                      {"name": "Activities", "amount": 100.00},
                      {"name": "Food", "amount": 200.00}
                    ],
                    "notes": "Estimates only"
                  },
                  "quality": {
                    "confidence": 0.88,
                    "warnings": ["Verify opening hours"],
                    "rationale": "Balanced pacing and costs"
                  }
                }
                """;
    }

    private TripCacheService cacheService() {
        return new TripCacheService(new ConcurrentMapCacheManager(
                TripCacheNames.TRIP_BY_ID,
                TripCacheNames.TRIP_LISTS,
                TripCacheNames.PLACE_RESOLUTIONS
        ));
    }
}
