package com.majjid.microservices.inventory.service.inventoryService;

import com.majjid.microservices.inventory.Dto.ResponseDto;
import com.majjid.microservices.inventory.Dto.inventory.*;
import com.majjid.microservices.inventory.config.CustomAppException;
import com.majjid.microservices.inventory.mappers.CustomMapper;
import com.majjid.microservices.inventory.model.Inventory;
import com.majjid.microservices.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService implements IInventoryService {
    private final InventoryRepository inventoryRepository;
    private final CustomMapper mapper;

    @Transactional(readOnly = true)
    @Override
    public ResponseDto<InventoryResponseDto> isInStock(String skuCode, IsInStockRequestDto requestDto) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        CustomAppException.buildNotFoundMsg(skuCode, "inventory")));

        log.info("get the inventory from the db  {}",inventory.getSkuCode());

        if(inventory.getQuantity() < requestDto.quantity()){
            throw  new CustomAppException(HttpStatus.CONFLICT, CustomAppException.buildNotEnoughMessage(skuCode, "inventory"));
        }

        InventoryResponseDto responseDto = mapper.toDto(inventory);

        return ResponseDto.success(responseDto,  "The inventory with the id "+skuCode+ " is In stock");
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDto<List<InventoryResponseDto>> getAllInventories() {
        List<Inventory> inventories = inventoryRepository.findAll();
        List<InventoryResponseDto> inventoryDtos = inventories.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseDto.listed(inventoryDtos, "inventories");
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseDto<InventoryResponseDto> getInventoryBySkuCode(String skuCode) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        CustomAppException.buildNotFoundMsg(skuCode, "inventory")));
        return ResponseDto.retrieved(mapper.toDto(inventory), "inventory");
    }

    @Transactional
    @Override
    public ResponseDto<InventoryResponseDto> createInventory(InventoryCreateRequestDto requestDto) {
        if (inventoryRepository.findBySkuCode(requestDto.skuCode()).isPresent()) {
            throw new CustomAppException(HttpStatus.CONFLICT, CustomAppException.BuildAlreadyExistsMsg(requestDto.skuCode(), "inventory"));
        }

        Inventory inventory = mapper.toEntity(requestDto);
        Inventory savedInventory = inventoryRepository.save(inventory);
        return ResponseDto.created(mapper.toDto(savedInventory), "Inventory created successfully");
    }

    @Transactional
    @Override
    public ResponseDto<InventoryResponseDto> updateInventory(String skuCode, InventoryUpdateRequestDto requestDto) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        CustomAppException.buildNotFoundMsg(skuCode, "inventory")));

        mapper.toDto(requestDto, inventory);
        Inventory updatedInventory = inventoryRepository.save(inventory);
        return ResponseDto.updated(mapper.toDto(updatedInventory), "Inventory updated successfully");
    }

    @Transactional
    @Override
    public ResponseDto<InventoryResponseDto> sellInventory(String skuCode, SellDto sellDto) {
        log.info("Sell Service Response: {}", sellDto);
//        if(true){
//            throw new CustomAppException(HttpStatus.INTERNAL_SERVER_ERROR,"Simulated exception for testing resilience");
//        }
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        CustomAppException.buildNotFoundMsg(skuCode, "inventory")));

        if (inventory.getQuantity() < sellDto.quantity()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST,
                    "Insufficient inventory. Available: " + inventory.getQuantity() +
                            ", Requested: " + sellDto.quantity());
        }

        inventory.setQuantity(inventory.getQuantity() - sellDto.quantity());
        Inventory updatedInventory = inventoryRepository.save(inventory);
        return ResponseDto.success(mapper.toDto(updatedInventory), "Inventory sold successfully");
    }

    @Transactional
    @Override
    public ResponseDto<InventoryResponseDto> purchaseInventory(String skuCode, PurchaseDto purchaseDto) {
        Inventory inventory = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND,
                        CustomAppException.buildNotFoundMsg(skuCode, "inventory")));

        inventory.setQuantity(inventory.getQuantity() + purchaseDto.quantity());
        Inventory updatedInventory = inventoryRepository.save(inventory);
        return ResponseDto.success(mapper.toDto(updatedInventory), "Inventory purchased successfully");
    }
}
