package com.tripgenie.trip.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateBudgetRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @DecimalMin("0.00") BigDecimal totalAmount,
        @Size(max = 4000) String notes,
        @NotNull List<@Valid BudgetCategoryRequest> categories
) {
}
