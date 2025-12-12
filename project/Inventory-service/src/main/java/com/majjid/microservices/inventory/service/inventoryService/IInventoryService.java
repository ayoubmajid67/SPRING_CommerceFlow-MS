package com.majjid.microservices.inventory.service.inventoryService;

import com.majjid.microservices.inventory.Dto.ResponseDto;
import com.majjid.microservices.inventory.Dto.inventory.*;

import java.util.List;

public interface IInventoryService {

    // Retrieve
    ResponseDto<InventoryResponseDto> isInStock(String skuCode, IsInStockRequestDto isInStockRequestDto);


    ResponseDto<List<InventoryResponseDto>> getAllInventories();

    ResponseDto<InventoryResponseDto> getInventoryBySkuCode(String skuCode);

    // Create
    ResponseDto<InventoryResponseDto> createInventory( InventoryCreateRequestDto requestDto);

    // Update
    ResponseDto<InventoryResponseDto> updateInventory(String skuCode, InventoryUpdateRequestDto requestDto);

    // Sell (reduce quantity)
    ResponseDto<InventoryResponseDto> sellInventory(String skuCode, SellDto sellDto);

    // Purchase (increase quantity)
    ResponseDto<InventoryResponseDto> purchaseInventory(String skuCode, PurchaseDto purchaseDto);

}

