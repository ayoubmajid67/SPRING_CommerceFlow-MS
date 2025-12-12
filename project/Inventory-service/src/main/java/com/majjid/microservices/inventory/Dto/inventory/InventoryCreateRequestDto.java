package com.majjid.microservices.inventory.Dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public  record InventoryCreateRequestDto(
       @NotBlank(message = "skuCode is required")
        String skuCode,
       @NotNull(message = "quantity is required")
       @Positive(message = "quantity must be greater than zero")
       Integer quantity

) {
}
