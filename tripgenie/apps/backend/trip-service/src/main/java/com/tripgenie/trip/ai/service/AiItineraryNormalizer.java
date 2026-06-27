package com.tripgenie.trip.ai.service;

import com.tripgenie.trip.ai.dto.AiItinerary;
import com.tripgenie.trip.domain.Budget;
import com.tripgenie.trip.domain.BudgetCategory;
import com.tripgenie.trip.domain.ItineraryDay;
import com.tripgenie.trip.domain.ItineraryItem;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AiItineraryNormalizer {

    public java.util.List<ItineraryDay> toDays(AiItinerary itinerary) {
        return itinerary.itineraryDays().stream().map(source -> {
            ItineraryDay day = new ItineraryDay();
            day.setDayNumber(source.dayNumber());
            day.setDate(source.date());
            day.setTitle(trimToNull(source.title()));
            day.setNotes(trimToNull(source.notes()));
            source.itineraryItems().stream().map(this::toItem).forEach(day::addItem);
            return day;
        }).toList();
    }

    public Budget toBudget(AiItinerary.Budget source) {
        Budget budget = new Budget();
        applyBudget(budget, source);
        return budget;
    }

    public void applyBudget(Budget budget, AiItinerary.Budget source) {
        budget.setCurrency(source.currency().toUpperCase(Locale.ROOT));
        budget.setTotalAmount(source.totalAmount());
        budget.setNotes(trimToNull(source.notes()));
        budget.replaceCategories(source.categories().stream().map(category -> {
            BudgetCategory result = new BudgetCategory();
            result.setName(category.name().trim());
            result.setAmount(category.amount());
            return result;
        }).toList());
    }

    private ItineraryItem toItem(AiItinerary.Item source) {
        ItineraryItem item = new ItineraryItem();
        item.setPosition(source.position());
        item.setTitle(source.title().trim());
        item.setDescription(trimToNull(source.description()));
        item.setLocation(trimToNull(source.location()));
        item.setStartTime(source.startTime());
        item.setEndTime(source.endTime());
        item.setEstimatedCost(source.estimatedCost());
        return item;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
