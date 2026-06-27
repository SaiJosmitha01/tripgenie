package com.tripgenie.trip.ai.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.ai.config.AiProperties;
import com.tripgenie.trip.ai.dto.AiGenerationContext;
import com.tripgenie.trip.ai.dto.AiProviderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class GroqAiProvider implements AiProvider {
    private static final String PROVIDER = "groq";
    private static final String SYSTEM_PROMPT = """
            You are TripGenie's itinerary planning engine. Return only one valid JSON object with no markdown.
            Follow the requested dates exactly and produce one itinerary day per calendar day.
            Use realistic travel times, avoid overlapping activities, and keep estimated costs within the stated budget.
            Weather guidance may be general text only; never claim access to live weather, maps, availability, or prices.
            The JSON must use exactly this structure:
            {
              "itinerary_days": [{
                "day_number": 1,
                "date": "YYYY-MM-DD",
                "title": "string",
                "notes": "string or null",
                "itinerary_items": [{
                  "position": 1,
                  "title": "string",
                  "description": "string or null",
                  "location": "string or null",
                  "start_time": "HH:mm:ss or null",
                  "end_time": "HH:mm:ss or null",
                  "estimated_cost": 0.00
                }]
              }],
              "budget": {
                "currency": "ISO-4217 code",
                "total_amount": 0.00,
                "categories": [{"name": "string", "amount": 0.00}],
                "notes": "string or null"
              },
              "quality": {
                "confidence": 0.0,
                "warnings": ["string"],
                "rationale": "short string"
              }
            }
            Do not add properties. confidence must be between 0 and 1. All costs must be non-negative.
            """;

    private final RestClient restClient;
    private final AiProperties properties;

    public GroqAiProvider(RestClient groqRestClient, AiProperties properties) {
        this.restClient = groqRestClient;
        this.properties = properties;
    }

    @Override
    public AiProviderResponse generateItinerary(AiGenerationContext context) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException("AI_PROVIDER_NOT_CONFIGURED",
                    "GROQ_API_KEY is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }

        GroqRequest request = new GroqRequest(
                properties.model(),
                List.of(new Message("system", SYSTEM_PROMPT), new Message("user", userPrompt(context))),
                new ResponseFormat("json_object"),
                0.3
        );
        try {
            GroqResponse response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(properties.apiKey()))
                    .body(request)
                    .retrieve()
                    .body(GroqResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null
                    || response.choices().getFirst().message().content() == null) {
                throw providerFailure("Groq returned an empty completion");
            }
            return new AiProviderResponse(PROVIDER, properties.model(), response.choices().getFirst().message().content());
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw providerFailure("Groq itinerary generation failed");
        }
    }

    private String userPrompt(AiGenerationContext context) {
        String prompt = """
                Generate an itinerary using these trusted trip inputs:
                destination: %s
                start_date: %s
                end_date: %s
                budget: %s %s
                travelers: %d
                travel_style: %s
                interests: %s
                user_travel_preferences: %s
                """.formatted(
                context.destination(), context.startDate(), context.endDate(),
                context.budget() == null ? "not specified" : context.budget(), context.currency(),
                context.travelers(), context.travelStyle(), context.interests(),
                context.userTravelPreferences() == null ? "none provided" : context.userTravelPreferences());
        if (context.previousInvalidResponse() == null) {
            return prompt;
        }
        return prompt + "\nYour previous response failed strict validation. Correct it and return the complete JSON only."
                + "\nPrevious response:\n" + context.previousInvalidResponse();
    }

    private BusinessException providerFailure(String message) {
        return new BusinessException("AI_PROVIDER_FAILURE", message, HttpStatus.BAD_GATEWAY);
    }

    record GroqRequest(
            String model,
            List<Message> messages,
            @JsonProperty("response_format") ResponseFormat responseFormat,
            double temperature
    ) {
    }

    record Message(String role, String content) {
    }

    record ResponseFormat(String type) {
    }

    record GroqResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }
}
