package com.majjid.microservices.inventory.Dto.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IsInStockRequestDto(
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity
) {
}
