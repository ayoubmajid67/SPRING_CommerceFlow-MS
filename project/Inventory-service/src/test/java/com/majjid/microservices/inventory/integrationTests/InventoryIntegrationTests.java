package com.majjid.microservices.inventory.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.majjid.microservices.inventory.config.MockMvcTestConfig;
import com.majjid.microservices.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import com.majjid.microservices.inventory.config.TestcontainersConfiguration;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, MockMvcTestConfig.class})
@DisplayName("Integration Tests for Inventory HTTP Methods")
public class InventoryIntegrationTests {

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setup() {
        inventoryRepository.deleteAll(); // Cleans DB in milliseconds
    }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/inventory";

    /**
     * Reusable helper function to create an inventory item.
     */
    private ResultActions createInventory(String skuCode, int quantity) throws Exception {
        Map<String, Object> payload = Map.of("skuCode", skuCode, "quantity", quantity);
        String requestBody = objectMapper.writeValueAsString(payload);

        return mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    @Test
    @DisplayName("POST /api/inventory - Should create a new inventory item")
    void testCreateInventory() throws Exception {
        String sku = "SKU-" + UUID.randomUUID();
        createInventory(sku, 100)
                .andDo(print()) // Log request and response
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.skuCode", is(sku)))
                .andExpect(jsonPath("$.data.quantity", is(100)));
    }

    @Test
    @DisplayName("GET /api/inventory/{skuCode} - Should retrieve an existing inventory item")
    void testGetInventoryBySkuCode() throws Exception {
        String sku = "SKU-GET-001";
        createInventory(sku, 50).andExpect(status().isCreated());

        mockMvc.perform(get(BASE_URL + "/" + sku))
                .andDo(print()) // Log request and response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.skuCode", is(sku)))
                .andExpect(jsonPath("$.data.quantity", is(50)));
    }

    @Test
    @DisplayName("PUT /api/inventory/{skuCode} - Should update an existing inventory item")
    void testUpdateInventory() throws Exception {
        String sku = "SKU-UPDATE-001";
        createInventory(sku, 100).andExpect(status().isCreated());

        Map<String, Object> updatedPayload = Map.of("quantity", 150);
        String updatedBody = objectMapper.writeValueAsString(updatedPayload);

        mockMvc.perform(put(BASE_URL + "/" + sku)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedBody))
                .andDo(print()) // Log request and response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quantity", is(150)));
    }

    @Test
    @DisplayName("POST /{sku}/sell - Should sell inventory and decrease quantity")
    void testSellInventory() throws Exception {
        String sku = "SKU-SELL-001";
        createInventory(sku, 100).andExpect(status().isCreated());

        Map<String, Object> sellPayload = Map.of("quantity", 30);
        String sellBody = objectMapper.writeValueAsString(sellPayload);

        mockMvc.perform(post(BASE_URL + "/" + sku + "/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sellBody))
                .andDo(print()) // Log request and response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quantity", is(70)));
    }

    @Test
    @DisplayName("POST /{sku}/purchase - Should purchase inventory and increase quantity")
    void testPurchaseInventory() throws Exception {
        String sku = "SKU-PURCHASE-001";
        createInventory(sku, 100).andExpect(status().isCreated());

        Map<String, Object> purchasePayload = Map.of("quantity", 50);
        String purchaseBody = objectMapper.writeValueAsString(purchasePayload);

        mockMvc.perform(post(BASE_URL + "/" + sku + "/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchaseBody))
                .andDo(print()) // Log request and response
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quantity", is(150)));
    }
}
