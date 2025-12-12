package com.majjid.microservices.inventory.mappers;


import com.majjid.microservices.inventory.Dto.inventory.InventoryCreateRequestDto;
import com.majjid.microservices.inventory.Dto.inventory.InventoryResponseDto;
import com.majjid.microservices.inventory.Dto.inventory.InventoryUpdateRequestDto;
import com.majjid.microservices.inventory.model.Inventory;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface InventoryMapper  {

    @Mapping(target = "isInStock", expression = "java(inventory.getQuantity() > 0)")
    InventoryResponseDto toDto(Inventory inventory);


    @Mapping(target = "isInStock", expression = "java(isInStock)")
    InventoryResponseDto toDto(Inventory inventory, boolean isInStock);

    Inventory toEntity(InventoryCreateRequestDto inventoryCreateRequestDto);

    /**
     * Updates an existing inventory entity with data from the DTO.
     * Only non-null fields from the DTO are mapped.
     * Existing fields in the inventory that are not in the DTO remain unchanged.
     *
     * @param requestDto the DTO containing update data
     * @param inventory the existing inventory entity to update (modified in-place)
     */
    void toDto(InventoryUpdateRequestDto requestDto, @MappingTarget Inventory inventory);
}
