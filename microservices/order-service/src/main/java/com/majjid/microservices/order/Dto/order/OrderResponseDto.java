package com.majjid.microservices.order.Dto.order;

import com.majjid.microservices.order.model.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderResponseDto(
        long id,
        String orderNumber,
        String skuCode,
        BigDecimal price,
        Integer quantity,
        OrderStatus orderStatus

){


}