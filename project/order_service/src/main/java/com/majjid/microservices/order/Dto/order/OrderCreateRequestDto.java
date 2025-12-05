package com.majjid.microservices.order.Dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderCreateRequestDto(

        @NotBlank(message = "skuCode is required")
         String orderNumber,
         @NotBlank(message = "skuCode is required")
         String skuCode,
         @NotNull(message = "price is required")
         @Positive
         BigDecimal price,
         @NotNull(message = "Quantity is required")
         @Positive
         Integer quantity

){


}
