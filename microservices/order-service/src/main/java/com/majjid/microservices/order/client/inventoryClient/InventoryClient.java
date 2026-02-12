package com.majjid.microservices.order.client.inventoryClient;

import com.majjid.microservices.order.client.inventoryClient.dto.*;
import jakarta.validation.Valid;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@HttpExchange
public interface InventoryClient {

    String SERVICE_NAME = "inventory";
    String CB_NAME = "inventory"; // matches resilience4j instance

    @GetExchange("/{skuCode}/in-stock")
    @CircuitBreaker(name = CB_NAME)
    @Retry(name = CB_NAME)
    InventoryInStockResponse isInStock(
            @PathVariable String skuCode,
            @Valid @RequestBody IsInStockRequestDto isInStockRequestDto
    );

    @PostExchange("/{skuCode}/sell")
    @CircuitBreaker(name = CB_NAME)
    @Retry(name = CB_NAME)
    InventoryInStockResponse sellInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody SellDto sellDto
    );

    @PostExchange("/{skuCode}/purchase")
    @CircuitBreaker(name = CB_NAME)
    @Retry(name = CB_NAME)
    InventoryInStockResponse purchaseInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody PurchaseDto purchaseDto
    );

    // --------------------------
    // Fallback Methods using records
    default InventoryInStockResponse isInStockFallback(String skuCode,
                                                       IsInStockRequestDto request,
                                                       Throwable t) {
        return new InventoryInStockResponse(
                null,
                "Fallback: inventory service unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                false
        );
    }

    default InventoryInStockResponse sellFallback(String skuCode,
                                                  SellDto sellDto,
                                                  Throwable t) {
        return new InventoryInStockResponse(
                null, // data is null in fallback
                "Fallback: sell service unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                false
        );
    }

    default InventoryInStockResponse purchaseFallback(String skuCode,
                                                      PurchaseDto purchaseDto,
                                                      Throwable t) {
        return new InventoryInStockResponse(
                null, // data is null in fallback
                "Fallback: purchase service unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                false
        );
    }
}
