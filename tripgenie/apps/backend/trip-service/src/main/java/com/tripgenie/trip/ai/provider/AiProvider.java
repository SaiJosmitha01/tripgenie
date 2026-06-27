package com.tripgenie.trip.ai.provider;

import com.tripgenie.trip.ai.dto.AiGenerationContext;
import com.tripgenie.trip.ai.dto.AiProviderResponse;

public interface AiProvider {
    AiProviderResponse generateItinerary(AiGenerationContext context);
}
