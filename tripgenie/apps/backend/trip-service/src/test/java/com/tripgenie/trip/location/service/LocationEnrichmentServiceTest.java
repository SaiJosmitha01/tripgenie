package com.tripgenie.trip.location.service;

import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.domain.ItineraryDay;
import com.tripgenie.trip.domain.ItineraryItem;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.dto.LocationEnrichmentResponse;
import com.tripgenie.trip.location.dto.PlaceResolution;
import com.tripgenie.trip.location.provider.MapsProvider;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationEnrichmentServiceTest {
    @Mock
    private TripRepository tripRepository;
    @Mock
    private MapsProvider mapsProvider;

    private LocationEnrichmentService service;
    private UUID userId;
    private UUID tripId;
    private ItineraryItem item;
    private Trip trip;

    @BeforeEach
    void setUp() {
        service = new LocationEnrichmentService(tripRepository, mapsProvider, new TripMapper());
        userId = UUID.randomUUID();
        tripId = UUID.randomUUID();
        item = itineraryItem(UUID.randomUUID(), "Louvre Museum", "Paris");
        trip = trip(userId, tripId, item);
    }

    @Test
    void enrichTripStoresResolvedLocationMetadata() {
        PlaceResolution resolution = new PlaceResolution(
                "Musee du Louvre",
                "Rue de Rivoli, 75001 Paris, France",
                new BigDecimal("48.8606111"),
                new BigDecimal("2.3376440"),
                "ChIJmQJIxlVv5kcRwsnV4W7dY3I",
                new BigDecimal("4.70")
        );
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(mapsProvider.providerName()).thenReturn("google");
        when(mapsProvider.resolvePlace("Paris")).thenReturn(Optional.of(resolution));

        LocationEnrichmentResponse response = service.enrichTrip(userId, tripId);

        assertThat(response.tripId()).isEqualTo(tripId);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.attemptedItems()).isEqualTo(1);
        assertThat(response.enrichedItems()).isEqualTo(1);
        assertThat(response.provider()).isEqualTo("google");
        assertThat(item.getLocation()).isEqualTo("Musee du Louvre");
        assertThat(item.getFormattedAddress()).isEqualTo("Rue de Rivoli, 75001 Paris, France");
        assertThat(item.getLatitude()).isEqualByComparingTo("48.8606111");
        assertThat(item.getLongitude()).isEqualByComparingTo("2.3376440");
        assertThat(item.getGooglePlaceId()).isEqualTo("ChIJmQJIxlVv5kcRwsnV4W7dY3I");
        assertThat(item.getPlaceRating()).isEqualByComparingTo("4.70");
        verify(tripRepository).save(trip);
    }

    @Test
    void enrichTripFallsBackToTitleWhenLocationIsMissing() {
        item.setLocation(null);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(mapsProvider.providerName()).thenReturn("noop");
        when(mapsProvider.resolvePlace("Louvre Museum")).thenReturn(Optional.empty());

        LocationEnrichmentResponse response = service.enrichTrip(userId, tripId);

        assertThat(response.attemptedItems()).isEqualTo(1);
        assertThat(response.enrichedItems()).isZero();
        assertThat(response.items().getFirst().query()).isEqualTo("Louvre Museum");
        assertThat(response.items().getFirst().enriched()).isFalse();
    }

    @Test
    void enrichTripRejectsNonOwner() {
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.enrichTrip(UUID.randomUUID(), tripId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("TRIP_ACCESS_DENIED");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }

    private Trip trip(UUID ownerId, UUID id, ItineraryItem item) {
        Trip value = new Trip();
        ReflectionTestUtils.setField(value, "id", id);
        value.setOwnerId(ownerId);
        value.setTitle("Paris");
        value.setDestination("Paris");
        value.setStartDate(LocalDate.of(2026, 8, 1));
        value.setEndDate(LocalDate.of(2026, 8, 3));
        ItineraryDay day = new ItineraryDay();
        ReflectionTestUtils.setField(day, "id", UUID.randomUUID());
        day.setDayNumber(1);
        day.setDate(value.getStartDate());
        day.addItem(item);
        value.replaceItinerary(java.util.List.of(day));
        return value;
    }

    private ItineraryItem itineraryItem(UUID id, String title, String location) {
        ItineraryItem value = new ItineraryItem();
        ReflectionTestUtils.setField(value, "id", id);
        value.setPosition(1);
        value.setTitle(title);
        value.setLocation(location);
        return value;
    }
}
