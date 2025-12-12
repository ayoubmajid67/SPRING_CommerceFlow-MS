package com.majjid.microservices.inventory.Dto.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public  record InventoryUpdateRequestDto(

        @NotNull(message = "quantity is required")
        @PositiveOrZero(message = "quantity must be greater than or equal to zero")
        Integer quantity

) {
}
