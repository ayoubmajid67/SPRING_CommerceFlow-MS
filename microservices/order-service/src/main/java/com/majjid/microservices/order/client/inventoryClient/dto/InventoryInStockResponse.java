package com.majjid.microservices.order.client.inventoryClient.dto;

import org.springframework.http.HttpStatus;

public record InventoryInStockResponse (
        // Note: The type of 'data' is CONCRETE (InventoryResponseDto), not T.
         InventoryResponseDto data,
         String message,
        HttpStatus status,
         boolean success

) {
}
