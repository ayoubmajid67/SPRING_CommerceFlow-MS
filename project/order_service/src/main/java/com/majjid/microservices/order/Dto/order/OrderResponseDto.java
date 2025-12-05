package com.majjid.microservices.order.Dto.order;

import java.math.BigDecimal;

public record OrderResponseDto(
        long id,
        String orderNumber,
        String skuCode,
        BigDecimal price,
        Integer quantity

){


}