package com.majjid.microservices.order.client.inventoryClient;


import com.fasterxml.jackson.databind.JsonNode;
import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryInStockResponse;
import com.majjid.microservices.order.client.inventoryClient.dto.IsInStockRequestDto;
import com.majjid.microservices.order.client.inventoryClient.dto.SellDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import  com.majjid.microservices.order.client.inventoryClient.dto.InventoryResponseDto;


@FeignClient(value = "inventory", url = "http://localhost:8082")
public interface InventoryClient {
public  static final  String SERVICE_NAME="inventory";

    @RequestMapping(method = RequestMethod.GET,value = "api/inventory/{skuCode}/in-stock")
    public InventoryInStockResponse isInStock(@PathVariable String skuCode, @RequestBody IsInStockRequestDto isInStockRequestDto);

    @RequestMapping(method = RequestMethod.POST, value = "api/inventory/{skuCode}/sell")
    public InventoryInStockResponse sellInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody SellDto sellDto);

}
