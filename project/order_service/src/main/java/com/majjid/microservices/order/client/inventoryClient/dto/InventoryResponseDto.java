package com.majjid.microservices.order.client.inventoryClient.dto;


public  record InventoryResponseDto(
        Long id,
        String skuCode,
        Integer quantity,
        Boolean isInStock
) {
}
