package com.majjid.microservices.product.Dto;


import java.math.BigDecimal;


public record ProductResponseDto(
        String id,
        String name,
        String description,
        BigDecimal price
) {

}
