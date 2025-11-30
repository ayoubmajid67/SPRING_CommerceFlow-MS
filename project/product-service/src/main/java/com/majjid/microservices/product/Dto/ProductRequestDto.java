package com.majjid.microservices.product.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;


public record ProductRequestDto(
        @NotBlank(message = "Product name is required.")
        String name,
        String description,
        @NotNull(message = "Product price is required.")
        @Positive(message = "Product price must be positive.")
        BigDecimal price
) {

}
