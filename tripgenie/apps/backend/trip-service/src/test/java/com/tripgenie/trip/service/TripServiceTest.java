package com.tripgenie.trip.service;

import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.domain.TripStatus;
import com.tripgenie.trip.dto.BudgetCategoryRequest;
import com.tripgenie.trip.dto.CreateTripRequest;
import com.tripgenie.trip.dto.ItineraryDayRequest;
import com.tripgenie.trip.dto.ItineraryItemRequest;
import com.tripgenie.trip.dto.PageResponse;
import com.tripgenie.trip.dto.TripResponse;
import com.tripgenie.trip.dto.TripSummaryResponse;
import com.tripgenie.trip.dto.UpdateBudgetRequest;
import com.tripgenie.trip.dto.UpdateItineraryRequest;
import com.tripgenie.trip.dto.UpdateTripRequest;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {
    @Mock
    private TripRepository tripRepository;

    private TripService tripService;
    private UUID userId;
    private UUID tripId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, new TripMapper());
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        trip = trip(userId, tripId);
    }

    @Test
    void createTripAssignsAuthenticatedOwnerAndDraftStatus() {
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TripResponse response = tripService.createTrip(userId, new CreateTripRequest(
                " Japan ",
                " Tokyo ",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 7),
                " Food and culture "
        ));

        assertThat(response.ownerId()).isEqualTo(userId);
        assertThat(response.status()).isEqualTo(TripStatus.DRAFT);
        assertThat(response.title()).isEqualTo("Japan");
        assertThat(response.destination()).isEqualTo("Tokyo");
    }

    @Test
    void getTripRejectsNonOwner() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.getTrip(UUID.randomUUID(), tripId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("TRIP_ACCESS_DENIED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    @Test
    void updateTripEnforcesStatusLifecycle() {
        trip.setStatus(TripStatus.COMPLETED);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        UpdateTripRequest request = new UpdateTripRequest(
                "Japan",
                "Tokyo",
                trip.getStartDate(),
                trip.getEndDate(),
                TripStatus.ACTIVE,
                null
        );

        assertThatThrownBy(() -> tripService.updateTrip(userId, tripId, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("INVALID_TRIP_STATUS_TRANSITION"));
    }

    @Test
    void updateItineraryReplacesDaysAndItems() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ItineraryItemRequest item = new ItineraryItemRequest(
                1,
                "Tsukiji Market",
                null,
                "Tokyo",
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                new BigDecimal("45.00"),
                null
        );
        UpdateItineraryRequest request = new UpdateItineraryRequest(List.of(
                new ItineraryDayRequest(1, trip.getStartDate(), "Arrival", null, List.of(item))
        ));

        TripResponse response = tripService.updateItinerary(userId, tripId, request);

        assertThat(response.itinerary()).hasSize(1);
        assertThat(response.itinerary().getFirst().items()).hasSize(1);
        assertThat(response.itinerary().getFirst().items().getFirst().title()).isEqualTo("Tsukiji Market");
    }

    @Test
    void updateBudgetCalculatesCategoryBreakdown() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateBudgetRequest request = new UpdateBudgetRequest(
                "usd",
                new BigDecimal("1000.00"),
                null,
                List.of(
                        new BudgetCategoryRequest("Hotels", new BigDecimal("500.00")),
                        new BudgetCategoryRequest("Food", new BigDecimal("200.00"))
                )
        );

        TripResponse response = tripService.updateBudget(userId, tripId, request);

        assertThat(response.budget().currency()).isEqualTo("USD");
        assertThat(response.budget().allocatedAmount()).isEqualByComparingTo("700.00");
        assertThat(response.budget().unallocatedAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void listTripsReturnsPageForOwnerFilters() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(tripRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Trip>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(trip), pageable, 1));

        PageResponse<TripSummaryResponse> response = tripService.listTrips(
                userId,
                TripStatus.DRAFT,
                "tok",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                pageable
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().id()).isEqualTo(tripId);
    }

    @Test
    void deleteTripDeletesOwnedTrip() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        tripService.deleteTrip(userId, tripId);

        verify(tripRepository).delete(trip);
    }

    private Trip trip(UUID ownerId, UUID id) {
        Trip value = new Trip();
        ReflectionTestUtils.setField(value, "id", id);
        value.setOwnerId(ownerId);
        value.setTitle("Japan");
        value.setDestination("Tokyo");
        value.setStartDate(LocalDate.of(2026, 9, 1));
        value.setEndDate(LocalDate.of(2026, 9, 7));
        value.setStatus(TripStatus.DRAFT);
        return value;
    }
}
