package com.majjid.microservices.order.order.unitTests;

import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.client.inventoryClient.InventoryClient;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryInStockResponse;
import com.majjid.microservices.order.client.inventoryClient.dto.InventoryResponseDto;
import com.majjid.microservices.order.client.inventoryClient.dto.PurchaseDto;
import com.majjid.microservices.order.client.inventoryClient.dto.SellDto;
import com.majjid.microservices.order.config.CustomAppException;
import com.majjid.microservices.order.config.hanlders.feignHanlders.FeignClientHandler;
import com.majjid.microservices.order.mappers.CustomMapper;
import com.majjid.microservices.order.model.Order;
import com.majjid.microservices.order.model.enums.OrderStatus;
import com.majjid.microservices.order.repository.OrderRepository;
import com.majjid.microservices.order.service.orderService.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("True Unit Tests for OrderService")
class  orderServiceUnitTests {

    private static final Logger log = LoggerFactory.getLogger(orderServiceUnitTests.class);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomMapper mapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("GET /orders - Should return all orders")
    void testGetOrders() {
        log.info("Testing retrieval of all orders");
        Order order1 = new Order();
        Order order2 = new Order();

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));
        when(mapper.toDto(any(Order.class))).thenReturn(
                new OrderResponseDto(1, "ORD-1", "SKU1", BigDecimal.valueOf(100), 5, OrderStatus.UNDER_PROCESS)
        );

        ResponseDto<List<OrderResponseDto>> response = orderService.getOrders();

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        log.info("Successfully retrieved {} orders : {}", response.getData().size(), response.getData());
    }

    @Test
    @DisplayName("GET /orders/{id} - Should return order when exists")
    void testGetOrderById_Success() {
        log.info("Testing retrieval of order by id");
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto(  1, "ORD-1", "SKU1", BigDecimal.valueOf(50), 2, OrderStatus.UNDER_PROCESS);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(mapper.toDto(order)).thenReturn(dto);

        ResponseDto<OrderResponseDto> response = orderService.getOrderById(1);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        log.info("Successfully retrieved order: \n {}", response.getData());
    }

    @Test
    @DisplayName("GET /orders/{id} - Should throw 404 when not found")
    void testGetOrderById_NotFound() {
        log.info("Testing retrieval of non-existent order");
        when(orderRepository.findById(99)).thenReturn(Optional.empty());

        CustomAppException ex = assertThrows(
                CustomAppException.class,
                () -> orderService.getOrderById(99)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        log.warn("Correctly threw 404 Not Found for order id 99");
    }

    @Test
    @DisplayName("POST /orders - Should place order successfully")
    void testPlaceOrder_Success() {
        log.info("Testing placing a new order");

        OrderCreateRequestDto requestDto =
                new OrderCreateRequestDto("ORD-1", "SKU1", BigDecimal.valueOf(100), 5);

        Order order = new Order();
        OrderResponseDto responseDto = new OrderResponseDto(  (long) 1, "ORD-1", "SKU1", BigDecimal.valueOf(100), 5, OrderStatus.UNDER_PROCESS);

        InventoryInStockResponse inventoryResponse = new InventoryInStockResponse(
                new InventoryResponseDto(   (long) 1, "SKU1", 10, true),
                "Inventory reserved successfully",
                HttpStatus.OK,
                true
        );

        when(mapper.toObject(requestDto)).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(mapper.toDto(order)).thenReturn(responseDto);

        try (MockedStatic<FeignClientHandler> mockedStatic =
                     mockStatic(FeignClientHandler.class)) {

            mockedStatic.when(() ->
                            FeignClientHandler.handleFeignCall(any(), any()))
                    .thenReturn(inventoryResponse);

            ResponseDto<OrderResponseDto> response =
                    orderService.placeAnOrder(requestDto);

            assertTrue(response.isSuccess());
            assertEquals("ORD-1", response.getData().orderNumber());
            verify(orderRepository, times(1)).save(order);
            log.info("Successfully placed order: {}", response.getData().orderNumber());
        }
    }

    @Test
    @DisplayName("DELETE /orders/{id} - Should delete order")
    void testDeleteOrder_Success() {
        log.info("Testing deletion of order");
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto(1, "ORD-1", "SKU1", BigDecimal.valueOf(100), 5, OrderStatus.UNDER_PROCESS);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(mapper.toDto(order)).thenReturn(dto);

        ResponseDto<OrderResponseDto> response =
                orderService.deleteAnOrder(1);

        assertTrue(response.isSuccess());
        verify(orderRepository).delete(order);
        log.info("Successfully deleted order: {}", dto.orderNumber());
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel - Should cancel order")
    void testCancelOrder_Success() {
        log.info("Testing cancellation of order");
        Order order = new Order();
        order.setSkuCode("SKU1");
        order.setQuantity(5);
        order.setOrderStatus(OrderStatus.UNDER_PROCESS);

        OrderResponseDto dto = new OrderResponseDto(1, "ORD-1", "SKU1", BigDecimal.valueOf(100), 5, OrderStatus.CANCELLED);

        InventoryInStockResponse inventoryResponse = new InventoryInStockResponse(
                new InventoryResponseDto(   (long) 1, "SKU1", 10, true),
                "Order cancelled and inventory restored",
                HttpStatus.OK,
                true
        );

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(mapper.toDto(order)).thenReturn(dto);

        try (MockedStatic<FeignClientHandler> mockedStatic =
                     mockStatic(FeignClientHandler.class)) {

            mockedStatic.when(() ->
                            FeignClientHandler.handleFeignCall(any(), any()))
                    .thenReturn(inventoryResponse);

            ResponseDto<OrderResponseDto> response =
                    orderService.cancelAnOrder(1);

            assertTrue(response.isSuccess());
            assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
            log.info("Successfully cancelled order: {}", dto.orderNumber());
        }
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel - Should throw conflict if status invalid")
    void testCancelOrder_Conflict() {
        log.info("Testing cancellation conflict for order");
        Order order = new Order();
        order.setOrderStatus(OrderStatus.COMPLETED);

        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        CustomAppException ex = assertThrows(
                CustomAppException.class,
                () -> orderService.cancelAnOrder(1)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        log.warn("Correctly threw conflict for cancelling order with status {}", order.getOrderStatus());
    }
}
