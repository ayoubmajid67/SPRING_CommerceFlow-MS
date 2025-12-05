package com.majjid.microservices.order;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;

import io.restassured.RestAssured;
import org.testcontainers.containers.MySQLContainer;

import static org.hamcrest.Matchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceApplicationTests {

    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.3.0");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    static {
        mySQLContainer.start();
    }

    @Test
    void shouldPlaceAnOrder() {
        String requestBody = """
                {
                  "orderNumber": "ORD-123",
                  "skuCode": "SKU-456",
                  "price": 10.50,
                  "quantity": 2
                }
            """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/orders")
                .then()
                .assertThat()
                .statusCode(201)
                .body("data.id", notNullValue())
                .body("data.orderNumber", equalTo("ORD-123"))
                .body("data.skuCode", equalTo("SKU-456"))
                .body("data.price", equalTo(10.50f)) // Use 'f' for float comparison
                .body("data.quantity", equalTo(2))
                .body("message", containsString("created with success"))
                .body("success", equalTo(true));
    }
}
