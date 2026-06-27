package com.tripgenie.trip.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripgenie.trip.ai.dto.AiGenerationContext;
import com.tripgenie.trip.ai.dto.AiItinerary;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

@Component
public class AiItineraryResponseValidator {
    private final ObjectMapper strictObjectMapper;
    private final Validator validator;

    public AiItineraryResponseValidator(ObjectMapper objectMapper, Validator validator) {
        this.strictObjectMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
    }

    public AiItinerary parseAndValidate(String rawResponse, AiGenerationContext context) {
        final AiItinerary itinerary;
        try {
            itinerary = strictObjectMapper.readValue(rawResponse, AiItinerary.class);
        } catch (JsonProcessingException exception) {
            throw new InvalidAiResponseException("Response is not valid itinerary JSON", exception);
        }

        Set<ConstraintViolation<AiItinerary>> violations = validator.validate(itinerary);
        if (!violations.isEmpty()) {
            throw new InvalidAiResponseException("Response violates the itinerary schema");
        }
        validateDays(itinerary, context);
        validateBudget(itinerary, context);
        return itinerary;
    }

    private void validateDays(AiItinerary itinerary, AiGenerationContext context) {
        long expectedDays = ChronoUnit.DAYS.between(context.startDate(), context.endDate()) + 1;
        if (itinerary.itineraryDays().size() != expectedDays) {
            throw new InvalidAiResponseException("Response does not contain every trip day");
        }
        Set<Integer> dayNumbers = new HashSet<>();
        for (AiItinerary.Day day : itinerary.itineraryDays()) {
            LocalDate expectedDate = context.startDate().plusDays(day.dayNumber() - 1L);
            if (!dayNumbers.add(day.dayNumber()) || !day.date().equals(expectedDate)) {
                throw new InvalidAiResponseException("Itinerary day numbering or dates are invalid");
            }
            Set<Integer> positions = new HashSet<>();
            for (AiItinerary.Item item : day.itineraryItems()) {
                if (!positions.add(item.position())) {
                    throw new InvalidAiResponseException("Itinerary item positions must be unique per day");
                }
                if (item.startTime() != null && item.endTime() != null
                        && item.endTime().isBefore(item.startTime())) {
                    throw new InvalidAiResponseException("Itinerary item time range is invalid");
                }
            }
        }
    }

    private void validateBudget(AiItinerary itinerary, AiGenerationContext context) {
        if (itinerary.budget() == null) {
            return;
        }
        BigDecimal allocated = itinerary.budget().categories().stream()
                .map(AiItinerary.Category::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (allocated.compareTo(itinerary.budget().totalAmount()) > 0) {
            throw new InvalidAiResponseException("Budget categories exceed the generated total");
        }
        if (context.budget() != null && itinerary.budget().totalAmount().compareTo(context.budget()) > 0) {
            throw new InvalidAiResponseException("Generated budget exceeds the requested budget");
        }
    }

    public static class InvalidAiResponseException extends RuntimeException {
        InvalidAiResponseException(String message) {
            super(message);
        }

        InvalidAiResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
