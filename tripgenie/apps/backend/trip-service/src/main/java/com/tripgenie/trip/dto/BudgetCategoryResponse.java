package com.tripgenie.trip.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetCategoryResponse(
        UUID id,
        String name,
        BigDecimal amount
) {
}
