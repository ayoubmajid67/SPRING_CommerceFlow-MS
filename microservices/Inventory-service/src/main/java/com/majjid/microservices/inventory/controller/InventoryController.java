package com.majjid.microservices.inventory.controller;

import com.majjid.microservices.inventory.Dto.ResponseDto;
import com.majjid.microservices.inventory.Dto.inventory.*;
import com.majjid.microservices.inventory.service.inventoryService.IInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final IInventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<List<InventoryResponseDto>> getAllInventories() {
        return inventoryService.getAllInventories();
    }

    @PostMapping("/{skuCode}/in-stock")
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<InventoryResponseDto> isInStock(
            @PathVariable String skuCode,
            @Valid @RequestBody IsInStockRequestDto requestDto) {
        return inventoryService.isInStock(skuCode, requestDto);
    }

    @GetMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<InventoryResponseDto> getInventoryBySkuCode(@PathVariable String skuCode) {
        return inventoryService.getInventoryBySkuCode(skuCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto<InventoryResponseDto> createInventory(
            @Valid @RequestBody InventoryCreateRequestDto requestDto) {
        return inventoryService.createInventory(requestDto);
    }

    @PutMapping("/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<InventoryResponseDto> updateInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody InventoryUpdateRequestDto requestDto) {
        return inventoryService.updateInventory(skuCode, requestDto);
    }

    @PostMapping("/{skuCode}/sell")
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<InventoryResponseDto> sellInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody SellDto sellDto) {
        return inventoryService.sellInventory(skuCode, sellDto);
    }

    @PostMapping("/{skuCode}/purchase")
    @ResponseStatus(HttpStatus.OK)
    public ResponseDto<InventoryResponseDto> purchaseInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody PurchaseDto purchaseDto) {
        return inventoryService.purchaseInventory(skuCode, purchaseDto);
    }
}
