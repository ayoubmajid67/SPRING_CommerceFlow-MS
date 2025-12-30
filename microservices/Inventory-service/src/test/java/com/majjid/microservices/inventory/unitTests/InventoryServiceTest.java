package com.majjid.microservices.inventory.unitTests;

import com.majjid.microservices.inventory.Dto.ResponseDto;
import com.majjid.microservices.inventory.Dto.inventory.*;
import com.majjid.microservices.inventory.config.CustomAppException;
import com.majjid.microservices.inventory.mappers.CustomMapper;
import com.majjid.microservices.inventory.model.Inventory;
import com.majjid.microservices.inventory.repository.InventoryRepository;
import com.majjid.microservices.inventory.service.inventoryService.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("True Unit Tests for InventoryService")
class InventoryServiceTest {

    private static final Logger log = LoggerFactory.getLogger(InventoryServiceTest.class);

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CustomMapper mapper;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("POST /{skuCode}/in-stock - Should return success when quantity is sufficient")
    void testIsInStock_Sufficient() {
        log.info("Testing isInStock with sufficient quantity");
        // Arrange
        String skuCode = "SKU-123";
        IsInStockRequestDto requestDto = new IsInStockRequestDto(10);
        Inventory fakeInventory = new Inventory(1L, skuCode, 100);
        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(fakeInventory));

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.isInStock(skuCode, requestDto);

        // Assert
        assertTrue(response.isSuccess());
        assertTrue(response.getData().isInStock());
        log.info("Successfully verified stock for SKU: {}", skuCode);
    }

    @Test
    @DisplayName("POST /{skuCode}/in-stock - Should throw exception when quantity is insufficient")
    void testIsInStock_Insufficient_ThrowsException() {
        log.info("Testing isInStock with insufficient quantity, expecting exception");
        // Arrange
        String skuCode = "SKU-123";
        IsInStockRequestDto requestDto = new IsInStockRequestDto(150);
        Inventory fakeInventory = new Inventory(1L, skuCode, 100);
        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(fakeInventory));

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class, () -> {
            inventoryService.isInStock(skuCode, requestDto);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().equals(CustomAppException.buildNotEnoughMessage(skuCode, "inventory")));
        log.warn("Correctly threw exception for insufficient stock on SKU: {}", skuCode);
    }

    @Test
    @DisplayName("GET /api/inventory - Should return all inventories")
    void testGetAllInventories() {
        log.info("Testing retrieval of all inventories");
        // Arrange
        Inventory inv1 = new Inventory(1L, "SKU1", 10);
        Inventory inv2 = new Inventory(2L, "SKU2", 20);
        when(inventoryRepository.findAll()).thenReturn(List.of(inv1, inv2));
        when(mapper.toDto(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            return new InventoryResponseDto(inv.getId(), inv.getSkuCode(), inv.getQuantity(), inv.getQuantity() > 0);
        });

        // Act
        ResponseDto<List<InventoryResponseDto>> response = inventoryService.getAllInventories();

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        log.info("Successfully retrieved {} inventories", response.getData().size());
    }

    @Test
    @DisplayName("GET /{skuCode} - Should return inventory when SKU code exists")
    void testGetInventoryBySkuCode_Success() {
        log.info("Testing retrieval of inventory by existing SKU code");
        // Arrange
        String skuCode = "SKU-123";
        Inventory fakeInventory = new Inventory(1L, skuCode, 100);
        InventoryResponseDto fakeDto = new InventoryResponseDto(1L, skuCode, 100, true);
        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(fakeInventory));
        when(mapper.toDto(fakeInventory)).thenReturn(fakeDto);

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.getInventoryBySkuCode(skuCode);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(skuCode, response.getData().skuCode());
        log.info("Successfully retrieved inventory for SKU: {}", skuCode);
    }

    @Test
    @DisplayName("GET /{skuCode} - Should throw 404 when SKU code does not exist")
    void testGetInventoryBySkuCode_NotFound() {
        log.info("Testing retrieval of inventory by non-existent SKU code");
        // Arrange
        String skuCode = "NON-EXISTENT-SKU";
        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.empty());

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class,
                () -> inventoryService.getInventoryBySkuCode(skuCode));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        log.warn("Correctly threw 404 Not Found for SKU: {}", skuCode);
    }

    @Test
    @DisplayName("POST /api/inventory - Should create a new inventory")
    void testCreateInventory_Success() {
        log.info("Testing creation of a new inventory");
        // Arrange
        InventoryCreateRequestDto requestDto = new InventoryCreateRequestDto("NEW-SKU", 50);
        Inventory newInventory = new Inventory(1L, "NEW-SKU", 50);
        InventoryResponseDto responseDto = new InventoryResponseDto(1L, "NEW-SKU", 50, true);

        when(inventoryRepository.findBySkuCode(requestDto.skuCode())).thenReturn(Optional.empty());
        when(mapper.toEntity(requestDto)).thenReturn(newInventory);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(newInventory);
        when(mapper.toDto(newInventory)).thenReturn(responseDto);

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.createInventory(requestDto);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals("NEW-SKU", response.getData().skuCode());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
        log.info("Successfully created inventory for SKU: {}", response.getData().skuCode());
    }

    @Test
    @DisplayName("POST /api/inventory - Should throw 409 Conflict for duplicate SKU")
    void testCreateInventory_Conflict() {
        log.info("Testing creation of inventory with a duplicate SKU");
        // Arrange
        InventoryCreateRequestDto requestDto = new InventoryCreateRequestDto("EXISTING-SKU", 50);
        when(inventoryRepository.findBySkuCode(requestDto.skuCode())).thenReturn(Optional.of(new Inventory()));

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class,
                () -> inventoryService.createInventory(requestDto));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        log.warn("Correctly threw 409 Conflict for duplicate SKU: {}", requestDto.skuCode());
    }

    @Test
    @DisplayName("PUT /{skuCode} - Should update an existing inventory")
    void testUpdateInventory_Success() {
        log.info("Testing update of an existing inventory");
        // Arrange
        String skuCode = "UPDATE-SKU";
        InventoryUpdateRequestDto requestDto = new InventoryUpdateRequestDto(200);
        Inventory existingInventory = new Inventory(1L, skuCode, 100);
        Inventory updatedInventory = new Inventory(1L, skuCode, 200);
        InventoryResponseDto responseDto = new InventoryResponseDto(1L, skuCode, 200, true);

        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
        when(mapper.toDto(updatedInventory)).thenReturn(responseDto);
        doNothing().when(mapper).toDto(requestDto, existingInventory);

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.updateInventory(skuCode, requestDto);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(200, response.getData().quantity());
        verify(inventoryRepository, times(1)).save(existingInventory);
        log.info("Successfully updated inventory for SKU: {}", skuCode);
    }

    @Test
    @DisplayName("POST /{skuCode}/sell - Should sell inventory successfully")
    void testSellInventory_Success() {
        log.info("Testing successful sale of inventory");
        // Arrange
        String skuCode = "SELL-SKU";
        SellDto sellDto = new SellDto(20);
        Inventory existingInventory = new Inventory(1L, skuCode, 100);
        Inventory updatedInventory = new Inventory(1L, skuCode, 80);
        InventoryResponseDto responseDto = new InventoryResponseDto(1L, skuCode, 80, true);

        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
        when(mapper.toDto(updatedInventory)).thenReturn(responseDto);

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.sellInventory(skuCode, sellDto);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(80, response.getData().quantity());
        verify(inventoryRepository, times(1)).save(existingInventory);
        log.info("Successfully sold 20 units from SKU: {}", skuCode);
    }

    @Test
    @DisplayName("POST /{skuCode}/sell - Should throw 400 Bad Request for insufficient stock")
    void testSellInventory_InsufficientStock() {
        log.info("Testing sale of inventory with insufficient stock");
        // Arrange
        String skuCode = "INSUFFICIENT-SKU";
        SellDto sellDto = new SellDto(150);
        Inventory existingInventory = new Inventory(1L, skuCode, 100);
        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(existingInventory));

        // Act & Assert
        CustomAppException exception = assertThrows(CustomAppException.class,
                () -> inventoryService.sellInventory(skuCode, sellDto));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        log.warn("Correctly threw 400 Bad Request for insufficient stock on SKU: {}", skuCode);
    }

    @Test
    @DisplayName("POST /{skuCode}/purchase - Should purchase inventory successfully")
    void testPurchaseInventory_Success() {
        log.info("Testing successful purchase of inventory");
        // Arrange
        String skuCode = "PURCHASE-SKU";
        PurchaseDto purchaseDto = new PurchaseDto(50);
        Inventory existingInventory = new Inventory(1L, skuCode, 100);
        Inventory updatedInventory = new Inventory(1L, skuCode, 150);
        InventoryResponseDto responseDto = new InventoryResponseDto(1L, skuCode, 150, true);

        when(inventoryRepository.findBySkuCode(skuCode)).thenReturn(Optional.of(existingInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(updatedInventory);
        when(mapper.toDto(updatedInventory)).thenReturn(responseDto);

        // Act
        ResponseDto<InventoryResponseDto> response = inventoryService.purchaseInventory(skuCode, purchaseDto);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(150, response.getData().quantity());
        verify(inventoryRepository, times(1)).save(existingInventory);
        log.info("Successfully purchased 50 units for SKU: {}", skuCode);
    }
}
