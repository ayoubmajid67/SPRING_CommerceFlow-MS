package com.majjid.microservices.inventory.E2ETests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majjid.microservices.inventory.repository.InventoryRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ✅ FIXED Integration Tests for Inventory Service
 * - All CREATE endpoints use POST /api/inventory with InventoryCreateRequestDto
 * - All UPDATE endpoints use PUT /api/inventory/{skuCode} with InventoryUpdateRequestDto
 * - All SELL endpoints use POST /api/inventory/{skuCode}/sell with SellDto
 * - All PURCHASE endpoints use POST /api/inventory/{skuCode}/purchase with PurchaseDto
 */


/*NOTES : ------------------------------------------------------------
*  ALL END-TO-END TESTS THAT INCLUDES  GET ,PUT REQUESTS WILL NOT WORK CAUSE THERE IS AN ISSUE WITH
* REST ASSURED LIBRARY SO i WILL KEEP THIS CODE AS IT IS TILL I FIND A SOLUTION OR AN ALTERNATIVE LIBRARY
* */

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Inventory Service Integration Tests")
class InventoryE2ETests {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    InventoryRepository inventoryRepository;

    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startContainer() {
        mySQLContainer.start();
    }

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        inventoryRepository.deleteAll();
    }

    // ✅ Helper methods to safely execute GET requests without NPE bug
    private Response safeGet(String path) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .get(path)
                .then()
                .extract()
                .response();
    }

    private Response safeGet(String path, Object... pathParams) {
        return RestAssured
                .given()
                .contentType(ContentType.JSON)
                .log().all()
                .when()
                .get(path, pathParams)
                .then()
                .extract()
                .response();
    }

    // ======================== GET ALL INVENTORIES TESTS ========================

    @Test
    @DisplayName("GET /api/inventory - Should return empty list initially")
    void testGetAllInventoriesEmpty() {
        Response response = safeGet("/api/inventory");
        response.then()
                .assertThat()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("retrieved successfully"))
                .body("data", hasSize(0));
    }

    // ======================== CREATE INVENTORY TESTS ========================

    @Test
    @DisplayName("POST /api/inventory - Should create new inventory")
    void testCreateInventory() {
        // ✅ CORRECT: Use InventoryCreateRequestDto format
        String requestBody = """
                {
                  "skuCode": "SKU-CREATE-001",
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .log().all()
                .when()
                .post("/api/inventory")  // ✅ POST to /api/inventory (not with SKU in path)
                .then()
                .log().all()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("message", containsString("created successfully"))
                .body("data.skuCode", equalTo("SKU-CREATE-001"))
                .body("data.quantity", equalTo(100))
                .body("data.isInStock", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/inventory - Should fail with invalid quantity")
    void testCreateInventoryWithNegativeQuantity() {
        // ✅ CORRECT: Negative quantity should fail validation
        String requestBody = """
                {
                  "skuCode": "SKU-NEG-QUANTITY",
                  "quantity": -50
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .log().all()
                .when()
                .post("/api/inventory")
                .then()
                .log().all()
                .statusCode(400);
    }

    @Test
    @DisplayName("POST /api/inventory - Should fail with duplicate SKU")
    void testCreateInventoryDuplicate() {
        String requestBody = """
                {
                  "skuCode": "SKU-DUP-CREATE",
                  "quantity": 50
                }
                """;

        // First creation
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .log().all()
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Try to create with same SKU again
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .log().all()
                .when()
                .post("/api/inventory")
                .then()
                .log().all()
                .statusCode(409)
                .body("success", equalTo(false));
    }

    // ======================== GET BY SKU CODE TESTS ========================

    @Test
    @DisplayName("GET /api/inventory/{skuCode} - Should retrieve inventory by SKU")
    void testGetInventoryBySkuCode() {
        // Create inventory first
        String createBody = """
                {
                  "skuCode": "SKU-GET-001",
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Get inventory
        Response response = safeGet("/api/inventory/SKU-GET-001");
        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.skuCode", equalTo("SKU-GET-001"))
                .body("data.quantity", equalTo(100))
                .body("data.isInStock", equalTo(true));
    }

    @Test
    @DisplayName("GET /api/inventory/{skuCode} - Should return 404 for non-existent SKU")
    void testGetInventoryBySkuCodeNotFound() {
        Response response = safeGet("/api/inventory/NON-EXISTENT");
        response.then()
                .statusCode(404)
                .body("success", equalTo(false));
    }

    // ======================== UPDATE INVENTORY TESTS ========================

    @Test
    @DisplayName("PUT /api/inventory/{skuCode} - Should update inventory quantity")
    void testUpdateInventory() {
        // Create inventory
        String createBody = """
                {
                  "skuCode": "SKU-UPDATE-001",
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .log().all()
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Update - ✅ CORRECT: Use InventoryUpdateRequestDto (only quantity field)
        String updateBody = """
                {
                  "quantity": 200
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .log().all()
                .when()
                .put("/api/inventory/SKU-UPDATE-001")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("updated successfully"))
                .body("data.quantity", equalTo(200));
    }

    @Test
    @DisplayName("PUT /api/inventory/{skuCode} - Should update quantity to zero")
    void testUpdateInventoryToZero() {
        // Create inventory
        String createBody = """
                {
                  "skuCode": "SKU-UPDATE-002",
                  "quantity": 50
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Update to zero - ✅ CORRECT: quantity >= 0 is valid
        String updateBody = """
                {
                  "quantity": 0
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .log().all()
                .when()
                .put("/api/inventory/SKU-UPDATE-002")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(0))
                .body("data.isInStock", equalTo(false));
    }

    // ======================== SELL INVENTORY TESTS ========================

    @Test
    @DisplayName("POST /api/inventory/{skuCode}/sell - Should reduce inventory quantity")
    void testSellInventory() {
        // Create inventory
        String createBody = """
                {
                  "skuCode": "SKU-SELL-001",
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Sell - ✅ CORRECT: Use SellDto (only quantity field, must be > 0)
        String sellBody = """
                {
                  "quantity": 30
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sellBody)
                .log().all()
                .when()
                .post("/api/inventory/SKU-SELL-001/sell")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("sold successfully"))
                .body("data.quantity", equalTo(70))
                .body("data.isInStock", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/inventory/{skuCode}/sell - Should mark as out of stock")
    void testSellInventoryOutOfStock() {
        // Create inventory
        String createBody = """
                {
                  "skuCode": "SKU-SELL-002",
                  "quantity": 25
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Sell all units
        String sellBody = """
                {
                  "quantity": 25
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sellBody)
                .log().all()
                .when()
                .post("/api/inventory/SKU-SELL-002/sell")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(0))
                .body("data.isInStock", equalTo(false));
    }

    @Test
    @DisplayName("POST /api/inventory/{skuCode}/sell - Should fail with insufficient inventory")
    void testSellInventoryInsufficientStock() {
        // Create inventory with 10 units
        String createBody = """
                {
                  "skuCode": "SKU-SELL-003",
                  "quantity": 10
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Try to sell more than available
        String sellBody = """
                {
                  "quantity": 50
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sellBody)
                .log().all()
                .when()
                .post("/api/inventory/SKU-SELL-003/sell")
                .then()
                .log().all()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", containsString("Insufficient"));
    }

    // ======================== PURCHASE INVENTORY TESTS ========================

    @Test
    @DisplayName("POST /api/inventory/{skuCode}/purchase - Should increase inventory quantity")
    void testPurchaseInventory() {
        // Create inventory
        String createBody = """
                {
                  "skuCode": "SKU-PURCHASE-001",
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Purchase - ✅ CORRECT: Use PurchaseDto (only quantity field, must be > 0)
        String purchaseBody = """
                {
                  "quantity": 50
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(purchaseBody)
                .log().all()
                .when()
                .post("/api/inventory/SKU-PURCHASE-001/purchase")
                .then()
                .log().all()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("purchased successfully"))
                .body("data.quantity", equalTo(150))
                .body("data.isInStock", equalTo(true));
    }

    @Test
    @DisplayName("POST /api/inventory/{skuCode}/purchase - Should restore stock from zero")
    void testPurchaseInventoryFromOutOfStock() {
        // Create inventory with zero
        String createBody = """
                {
                  "skuCode": "SKU-PURCHASE-002",
                  "quantity": 0
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/api/inventory")
                .then()
                .statusCode(201);

        // Purchase to restore
        String purchaseBody = """
                {
                  "quantity": 40
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(purchaseBody)
                .log().all()
                .when()
                .post("/api/inventory/SKU-PURCHASE-002/purchase")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(40))
                .body("data.isInStock", equalTo(true));
    }

    // ======================== COMPLETE WORKFLOW TEST ========================

    @Test
    @DisplayName("Complete workflow - Create, Get, Update, Sell, Purchase")
    void testCompleteInventoryWorkflow() {
        String skuCode = "SKU-WORKFLOW-001";

        // 1. Create with 200 units
        String createBody = """
                {
                  "skuCode": "SKU-WORKFLOW-001",
                  "quantity": 200
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBody)
                .log().all()
                .when()
                .post("/api/inventory")
                .then()
                .log().all()
                .statusCode(201)
                .body("data.quantity", equalTo(200));

        // 2. Get inventory
        Response getResponse = safeGet("/api/inventory/" + skuCode);
        getResponse.then()
                .statusCode(200)
                .body("data.skuCode", equalTo(skuCode))
                .body("data.quantity", equalTo(200));

        // 3. Sell 50 units (should have 150)
        String sellBody = """
                {
                  "quantity": 50
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(sellBody)
                .log().all()
                .when()
                .post("/api/inventory/" + skuCode + "/sell")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(150));

        // 4. Update to 300
        String updateBody = """
                {
                  "quantity": 300
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(updateBody)
                .log().all()
                .when()
                .put("/api/inventory/" + skuCode)
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(300));

        // 5. Purchase 100 units (should have 400)
        String purchaseBody = """
                {
                  "quantity": 100
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(purchaseBody)
                .log().all()
                .when()
                .post("/api/inventory/" + skuCode + "/purchase")
                .then()
                .log().all()
                .statusCode(200)
                .body("data.quantity", equalTo(400))
                .body("data.isInStock", equalTo(true));

        // 6. Verify final state
        Response finalResponse = safeGet("/api/inventory/" + skuCode);
        finalResponse.then()
                .statusCode(200)
                .body("data.quantity", equalTo(400))
                .body("data.isInStock", equalTo(true));
    }
}

