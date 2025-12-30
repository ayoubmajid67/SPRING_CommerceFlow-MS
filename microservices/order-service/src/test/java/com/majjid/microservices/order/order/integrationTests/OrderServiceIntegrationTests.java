package com.majjid.microservices.order.order.integrationTests;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.majjid.microservices.order.Dto.ResponseDto;
import com.majjid.microservices.order.Dto.order.OrderCreateRequestDto;
import com.majjid.microservices.order.Dto.order.OrderResponseDto;
import com.majjid.microservices.order.client.inventoryClient.InventoryProperties;
import com.majjid.microservices.order.model.enums.OrderStatus;
import com.majjid.microservices.order.repository.OrderRepository;
import com.majjid.microservices.order.service.orderService.OrderService;
import com.majjid.microservices.order.order.stubs.InventoryClientStub;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderServiceIntegrationTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClientStub inventoryStub;

    @Autowired
    private InventoryProperties inventoryProperties;


    static WireMockServer wireMockServer;


    /*
    * @SpringBootTest
  |
  |--> ApplicationContext STARTS
        |
        |--> Feign clients created
              |
              |--> inventory.url resolved ONCE
  |
  |--> @BeforeAll runs (TOO LATE ❌)
  *
  * @DynamicPropertySource runs BEFORE this chain.

    * */
    @DynamicPropertySource
    static void registerInventoryUrl(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();

        registry.add(
                "inventory.url",
                () -> "http://localhost:" + wireMockServer.port() + "/api/inventory/"
        );

        log.info("WireMock Inventory URL registered BEFORE context startup");
    }



    @BeforeAll
    void startWireMock() {

        inventoryStub.configureInventoryStub(wireMockServer.port());
        log.info("the base url of InventoryStub is : " + inventoryStub.getBaseUrl());
        log.info("WireMock server started on http://localhost:  " + wireMockServer.port());

        log.info("Inventory Service URL: " + inventoryProperties.buildInventoryServiceUrl(wireMockServer.port()));

    }
    @AfterAll
    void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        inventoryStub.reset();
    }

    @Test
    @DisplayName("Place an order successfully")
    void testPlaceOrder() {
        inventoryStub.stubSellInventory("SKU1", 5);

        OrderCreateRequestDto request = new OrderCreateRequestDto(
                "ORD-1", "SKU1", BigDecimal.valueOf(100), 5
        );

        ResponseDto<OrderResponseDto> response = orderService.placeAnOrder(request);

        assertTrue(response.isSuccess());
        assertEquals("ORD-1", response.getData().orderNumber());
        assertEquals(OrderStatus.UNDER_PROCESS, response.getData().orderStatus());
    }

    @Test
    @DisplayName("Cancel an order successfully")
    void testCancelOrder() {
        inventoryStub.stubSellInventory("SKU2", 3);
        inventoryStub.stubPurchaseInventory("SKU2", 3);

        OrderCreateRequestDto createRequest = new OrderCreateRequestDto(
                "ORD-2", "SKU2", BigDecimal.valueOf(200), 3
        );

        // Place order
        ResponseDto<OrderResponseDto> placed = orderService.placeAnOrder(createRequest);

        // Cancel order
        ResponseDto<OrderResponseDto> cancelled = orderService.cancelAnOrder( (int) placed.getData().id());

        assertTrue(cancelled.isSuccess());
        assertEquals(OrderStatus.CANCELLED, cancelled.getData().orderStatus());
    }
}
