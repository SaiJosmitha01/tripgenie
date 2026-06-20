package com.tripgenie.trip.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        String currency,
        BigDecimal totalAmount,
        BigDecimal allocatedAmount,
        BigDecimal unallocatedAmount,
        String notes,
        List<BudgetCategoryResponse> categories
) {
}
