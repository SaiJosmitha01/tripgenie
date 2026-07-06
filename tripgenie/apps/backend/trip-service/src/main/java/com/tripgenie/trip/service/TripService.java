package com.tripgenie.trip.service;

import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.common.exception.NotFoundException;
import com.tripgenie.trip.cache.TripCacheService;
import com.tripgenie.trip.domain.Budget;
import com.tripgenie.trip.domain.BudgetCategory;
import com.tripgenie.trip.domain.ItineraryDay;
import com.tripgenie.trip.domain.ItineraryItem;
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
import com.tripgenie.trip.event.TripEventPublisher;
import com.tripgenie.trip.mapper.TripMapper;
import com.tripgenie.trip.repository.TripRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TripService {
    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final TripEventPublisher tripEventPublisher;
    private final TripCacheService tripCacheService;
    private final MeterRegistry meterRegistry;

    public TripService(TripRepository tripRepository,
                       TripMapper tripMapper,
                       TripEventPublisher tripEventPublisher,
                       TripCacheService tripCacheService,
                       MeterRegistry meterRegistry) {
        this.tripRepository = tripRepository;
        this.tripMapper = tripMapper;
        this.tripEventPublisher = tripEventPublisher;
        this.tripCacheService = tripCacheService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public TripResponse createTrip(UUID userId, CreateTripRequest request) {
        return recordTripCrud("create", () -> {
            validateDateRange(request.startDate(), request.endDate());
            Trip trip = new Trip();
            trip.setOwnerId(userId);
            trip.setTitle(request.title().trim());
            trip.setDestination(request.destination().trim());
            trip.setStartDate(request.startDate());
            trip.setEndDate(request.endDate());
            trip.setDescription(trimToNull(request.description()));
            trip.setStatus(TripStatus.DRAFT);
            Trip savedTrip = tripRepository.save(trip);
            tripEventPublisher.publishTripCreated(savedTrip);
            tripCacheService.evictTripLists();
            return tripMapper.toResponse(savedTrip);
        });
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID userId, UUID tripId) {
        return recordTripCrud("get", () ->
                tripCacheService.getTrip(userId, tripId, () -> tripMapper.toResponse(findOwnedTrip(userId, tripId))));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripSummaryResponse> listTrips(
            UUID userId,
            TripStatus status,
            String destination,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            Pageable pageable
    ) {
        return recordTripCrud("list", () -> tripCacheService.listTrips(
                userId, status, destination, startDateFrom, startDateTo, pageable, () -> {
                    if (startDateFrom != null && startDateTo != null && startDateTo.isBefore(startDateFrom)) {
                        throw badRequest("INVALID_FILTER_DATE_RANGE", "startDateTo must be on or after startDateFrom");
                    }
                    Specification<Trip> specification =
                            tripSpecification(userId, status, destination, startDateFrom, startDateTo);
                    Page<TripSummaryResponse> page = tripRepository.findAll(specification, pageable)
                            .map(tripMapper::toSummary);
                    return PageResponse.from(page);
                }));
    }

    @Transactional
    public TripResponse updateTrip(UUID userId, UUID tripId, UpdateTripRequest request) {
        return recordTripCrud("update", () -> {
            validateDateRange(request.startDate(), request.endDate());
            Trip trip = findOwnedTrip(userId, tripId);
            if (!trip.getStatus().canTransitionTo(request.status())) {
                throw badRequest("INVALID_TRIP_STATUS_TRANSITION",
                        "Trip status cannot transition from " + trip.getStatus() + " to " + request.status());
            }
            trip.setTitle(request.title().trim());
            trip.setDestination(request.destination().trim());
            trip.setStartDate(request.startDate());
            trip.setEndDate(request.endDate());
            trip.setDescription(trimToNull(request.description()));
            trip.setStatus(request.status());
            validateExistingItineraryDates(trip);
            Trip savedTrip = tripRepository.save(trip);
            tripEventPublisher.publishTripUpdated(savedTrip);
            tripCacheService.evictTrip(userId, tripId);
            return tripMapper.toResponse(savedTrip);
        });
    }

    @Transactional
    public void deleteTrip(UUID userId, UUID tripId) {
        recordTripCrud("delete", () -> {
            tripRepository.delete(findOwnedTrip(userId, tripId));
            tripCacheService.evictTrip(userId, tripId);
            return null;
        });
    }

    @Transactional
    public TripResponse updateItinerary(UUID userId, UUID tripId, UpdateItineraryRequest request) {
        return recordTripCrud("update_itinerary", () -> {
            Trip trip = findOwnedTrip(userId, tripId);
            validateItinerary(trip, request.days());
            List<ItineraryDay> days = request.days().stream().map(this::toItineraryDay).toList();
            trip.replaceItinerary(days);
            TripResponse response = tripMapper.toResponse(tripRepository.save(trip));
            tripCacheService.evictTrip(userId, tripId);
            return response;
        });
    }

    @Transactional
    public TripResponse updateBudget(UUID userId, UUID tripId, UpdateBudgetRequest request) {
        return recordTripCrud("update_budget", () -> {
            Trip trip = findOwnedTrip(userId, tripId);
            validateBudget(request);
            Budget budget = trip.getBudget();
            if (budget == null) {
                budget = new Budget();
                trip.setBudget(budget);
            }
            budget.setCurrency(request.currency().toUpperCase(Locale.ROOT));
            budget.setTotalAmount(request.totalAmount());
            budget.setNotes(trimToNull(request.notes()));
            budget.replaceCategories(request.categories().stream().map(this::toBudgetCategory).toList());
            TripResponse response = tripMapper.toResponse(tripRepository.save(trip));
            tripCacheService.evictTrip(userId, tripId);
            return response;
        });
    }

    private <T> T recordTripCrud(String operation, java.util.function.Supplier<T> supplier) {
        return Timer.builder("tripgenie.trip.crud")
                .tag("operation", operation)
                .register(meterRegistry)
                .record(supplier);
    }

    private Trip findOwnedTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("TRIP_NOT_FOUND", "Trip was not found"));
        if (!trip.getOwnerId().equals(userId)) {
            throw new BusinessException("TRIP_ACCESS_DENIED", "You do not own this trip", HttpStatus.FORBIDDEN);
        }
        return trip;
    }

    private Specification<Trip> tripSpecification(
            UUID userId,
            TripStatus status,
            String destination,
            LocalDate startDateFrom,
            LocalDate startDateTo
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("ownerId"), userId));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (destination != null && !destination.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("destination")),
                        "%" + destination.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (startDateFrom != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("startDate"), startDateFrom));
            }
            if (startDateTo != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("startDate"), startDateTo));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ItineraryDay toItineraryDay(ItineraryDayRequest request) {
        ItineraryDay day = new ItineraryDay();
        day.setDayNumber(request.dayNumber());
        day.setDate(request.date());
        day.setTitle(trimToNull(request.title()));
        day.setNotes(trimToNull(request.notes()));
        request.items().stream().map(this::toItineraryItem).forEach(day::addItem);
        return day;
    }

    private ItineraryItem toItineraryItem(ItineraryItemRequest request) {
        ItineraryItem item = new ItineraryItem();
        item.setPosition(request.position());
        item.setTitle(request.title().trim());
        item.setDescription(trimToNull(request.description()));
        item.setLocation(trimToNull(request.location()));
        item.setStartTime(request.startTime());
        item.setEndTime(request.endTime());
        item.setEstimatedCost(request.estimatedCost());
        item.setBookingReference(trimToNull(request.bookingReference()));
        return item;
    }

    private BudgetCategory toBudgetCategory(BudgetCategoryRequest request) {
        BudgetCategory category = new BudgetCategory();
        category.setName(request.name().trim());
        category.setAmount(request.amount());
        return category;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw badRequest("INVALID_TRIP_DATE_RANGE", "endDate must be on or after startDate");
        }
    }

    private void validateExistingItineraryDates(Trip trip) {
        boolean outsideRange = trip.getItineraryDays().stream()
                .anyMatch(day -> day.getDate().isBefore(trip.getStartDate()) || day.getDate().isAfter(trip.getEndDate()));
        if (outsideRange) {
            throw badRequest("ITINERARY_OUTSIDE_TRIP_DATES",
                    "Existing itinerary days must remain within the updated trip date range");
        }
    }

    private void validateItinerary(Trip trip, List<ItineraryDayRequest> days) {
        Set<Integer> dayNumbers = new HashSet<>();
        for (ItineraryDayRequest day : days) {
            if (!dayNumbers.add(day.dayNumber())) {
                throw badRequest("DUPLICATE_ITINERARY_DAY", "Itinerary day numbers must be unique");
            }
            if (day.date().isBefore(trip.getStartDate()) || day.date().isAfter(trip.getEndDate())) {
                throw badRequest("ITINERARY_DAY_OUTSIDE_TRIP", "Itinerary days must be within the trip date range");
            }
            Set<Integer> positions = new HashSet<>();
            for (ItineraryItemRequest item : day.items()) {
                if (!positions.add(item.position())) {
                    throw badRequest("DUPLICATE_ITINERARY_POSITION", "Item positions must be unique within a day");
                }
                if (item.startTime() != null && item.endTime() != null && item.endTime().isBefore(item.startTime())) {
                    throw badRequest("INVALID_ITINERARY_TIME_RANGE", "Item endTime must be on or after startTime");
                }
            }
        }
    }

    private void validateBudget(UpdateBudgetRequest request) {
        Set<String> categoryNames = new HashSet<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (BudgetCategoryRequest category : request.categories()) {
            String normalizedName = category.name().trim().toLowerCase(Locale.ROOT);
            if (!categoryNames.add(normalizedName)) {
                throw badRequest("DUPLICATE_BUDGET_CATEGORY", "Budget category names must be unique");
            }
            allocated = allocated.add(category.amount());
        }
        if (allocated.compareTo(request.totalAmount()) > 0) {
            throw badRequest("BUDGET_OVERALLOCATED", "Budget category amounts cannot exceed the total amount");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }
}
