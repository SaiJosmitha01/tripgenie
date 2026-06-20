package com.tripgenie.trip.mapper;

import com.tripgenie.trip.domain.Budget;
import com.tripgenie.trip.domain.BudgetCategory;
import com.tripgenie.trip.domain.ItineraryDay;
import com.tripgenie.trip.domain.ItineraryItem;
import com.tripgenie.trip.domain.Trip;
import com.tripgenie.trip.dto.BudgetCategoryResponse;
import com.tripgenie.trip.dto.BudgetResponse;
import com.tripgenie.trip.dto.ItineraryDayResponse;
import com.tripgenie.trip.dto.ItineraryItemResponse;
import com.tripgenie.trip.dto.TripResponse;
import com.tripgenie.trip.dto.TripSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TripMapper {

    public TripResponse toResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getOwnerId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getDescription(),
                trip.getItineraryDays().stream().map(this::toItineraryDayResponse).toList(),
                toBudgetResponse(trip.getBudget()),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }

    public TripSummaryResponse toSummary(Trip trip) {
        return new TripSummaryResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getDestination(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getStatus(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }

    private ItineraryDayResponse toItineraryDayResponse(ItineraryDay day) {
        return new ItineraryDayResponse(
                day.getId(),
                day.getDayNumber(),
                day.getDate(),
                day.getTitle(),
                day.getNotes(),
                day.getItems().stream().map(this::toItineraryItemResponse).toList()
        );
    }

    private ItineraryItemResponse toItineraryItemResponse(ItineraryItem item) {
        return new ItineraryItemResponse(
                item.getId(),
                item.getPosition(),
                item.getTitle(),
                item.getDescription(),
                item.getLocation(),
                item.getStartTime(),
                item.getEndTime(),
                item.getEstimatedCost(),
                item.getBookingReference()
        );
    }

    private BudgetResponse toBudgetResponse(Budget budget) {
        if (budget == null) {
            return null;
        }
        BigDecimal allocated = budget.getCategories().stream()
                .map(BudgetCategory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BudgetResponse(
                budget.getId(),
                budget.getCurrency(),
                budget.getTotalAmount(),
                allocated,
                budget.getTotalAmount().subtract(allocated),
                budget.getNotes(),
                budget.getCategories().stream().map(this::toBudgetCategoryResponse).toList()
        );
    }

    private BudgetCategoryResponse toBudgetCategoryResponse(BudgetCategory category) {
        return new BudgetCategoryResponse(category.getId(), category.getName(), category.getAmount());
    }
}
