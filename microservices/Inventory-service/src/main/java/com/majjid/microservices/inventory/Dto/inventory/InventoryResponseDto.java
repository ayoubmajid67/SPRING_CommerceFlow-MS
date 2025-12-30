package  com.majjid.microservices.inventory.Dto.inventory;

public  record InventoryResponseDto(
         Long id,
         String skuCode,
         Integer quantity,
         Boolean isInStock
) {


}
