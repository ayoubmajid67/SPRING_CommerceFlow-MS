package com.majjid.microservices.order.client.inventoryClient;



import com.majjid.microservices.order.client.inventoryClient.dto.*;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@FeignClient(value = "inventory", url = "${inventory.url}")
public interface InventoryClient {
public  static final  String SERVICE_NAME="inventory";


    @RequestMapping(method = RequestMethod.GET,value = "{skuCode}/in-stock")
    public InventoryInStockResponse isInStock(@PathVariable String skuCode, @RequestBody IsInStockRequestDto isInStockRequestDto);

    @RequestMapping(method = RequestMethod.POST, value = "{skuCode}/sell")
    public InventoryInStockResponse sellInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody SellDto sellDto);

    @RequestMapping(method = RequestMethod.POST, value = "{skuCode}/purchase")
    public InventoryInStockResponse purchaseInventory(
            @PathVariable String skuCode,
            @Valid @RequestBody PurchaseDto purchaseDto);

}
