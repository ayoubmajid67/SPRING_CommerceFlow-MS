# Part 4 - Spring Cloud OpenFeign & Testing with WireMock

## Document Reference

- **Video Source:** [Spring Boot Microservices Tutorial - Part 4](https://youtu.be/GpqnYd8VR3k?si=YoLhw6PUyE1ELVRZ)
- **Topic:** Spring Cloud OpenFeign & WireMock Testing
- **Part:** 4 of Spring Boot Microservices Tutorial Series

---

## Table of Contents

1. [Introduction to OpenFeign](#introduction-to-openfeign)
2. [Why Use OpenFeign?](#why-use-openfeign)
3. [Setting Up OpenFeign](#setting-up-openfeign)
4. [Creating Feign Clients](#creating-feign-clients)
5. [WireMock for Testing](#wiremock-for-testing)
6. [Best Practices](#best-practices)

---

## Introduction to OpenFeign

**OpenFeign** (formerly known as Feign) is a declarative HTTP client developed by Netflix and now part of the Spring Cloud ecosystem. It simplifies the process of making HTTP requests to other microservices by allowing you to define REST clients using simple Java interfaces.

### Key Features

- **Declarative Syntax**: Define HTTP clients using annotations, similar to Spring MVC controllers
- **Integration with Spring Cloud**: Works seamlessly with Eureka, Ribbon, and other Spring Cloud components
- **Built-in Load Balancing**: Automatically integrates with client-side load balancing
- **Error Handling**: Customizable error decoders for handling HTTP errors
- **Logging**: Built-in request/response logging capabilities

---

## Why Use OpenFeign?

### The Problem: Traditional RestTemplate Approach

Before OpenFeign, developers typically used `RestTemplate` or `WebClient` to make HTTP calls between microservices:

```java
@Service
public class OrderService {
    
    private final RestTemplate restTemplate;
    
    public OrderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public InventoryResponse checkInventory(String skuCode) {
        String url = "http://inventory-service/api/inventory/" + skuCode;
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);
        return response.getBody();
    }
}
```

**Problems with this approach:**
- ❌ Boilerplate code for every HTTP call
- ❌ Manual URL construction
- ❌ Error handling scattered across the codebase
- ❌ Difficult to test and mock
- ❌ No type safety for request/response

### The Solution: OpenFeign

With OpenFeign, the same functionality becomes:

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse checkInventory(@PathVariable String skuCode);
}
```

**Benefits:**
- ✅ Clean, declarative interface
- ✅ Automatic URL construction
- ✅ Type-safe requests and responses
- ✅ Easy to test with WireMock
- ✅ Built-in retry and circuit breaker support

---

## Setting Up OpenFeign

### Step 1: Add Dependencies

Add the Spring Cloud OpenFeign dependency to your `pom.xml`:

```xml
<dependencies>
    <!-- Spring Cloud OpenFeign -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    
    <!-- For Service Discovery (if using Eureka) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 2: Enable Feign Clients

Add `@EnableFeignClients` to your main application class:

```java
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### Step 3: Configure Application Properties

```yaml
spring:
  application:
    name: order-service

# Feign Configuration
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 5000
        loggerLevel: basic
  
# If using Eureka for service discovery
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

---

## Creating Feign Clients

### Basic Feign Client

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse getInventory(@PathVariable("skuCode") String skuCode);
    
    @PostMapping("/api/inventory")
    InventoryResponse createInventory(@RequestBody InventoryRequest request);
    
    @PutMapping("/api/inventory/{id}")
    InventoryResponse updateInventory(
        @PathVariable("id") Long id, 
        @RequestBody InventoryRequest request
    );
    
    @DeleteMapping("/api/inventory/{id}")
    void deleteInventory(@PathVariable("id") Long id);
}
```

### Feign Client with URL (Without Service Discovery)

If you're not using Eureka, you can specify the URL directly:

```java
@FeignClient(
    name = "inventory-service",
    url = "${inventory.service.url:http://localhost:8082}"
)
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse getInventory(@PathVariable("skuCode") String skuCode);
}
```

### Using the Feign Client in a Service

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private final InventoryClient inventoryClient;
    private final OrderRepository orderRepository;
    
    public OrderResponse createOrder(OrderRequest orderRequest) {
        // Check inventory using Feign client
        InventoryResponse inventory = inventoryClient.getInventory(orderRequest.getSkuCode());
        
        if (inventory.getQuantity() < orderRequest.getQuantity()) {
            throw new InsufficientInventoryException("Not enough stock available");
        }
        
        // Create order
        Order order = Order.builder()
            .skuCode(orderRequest.getSkuCode())
            .quantity(orderRequest.getQuantity())
            .status("PENDING")
            .build();
            
        Order savedOrder = orderRepository.save(order);
        
        return mapToOrderResponse(savedOrder);
    }
}
```

### Custom Feign Configuration

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("User-Agent", "Order-Service");
            requestTemplate.header("Accept", "application/json");
        };
    }
}
```

### Custom Error Decoder

```java
public class CustomErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultErrorDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
                return new BadRequestException("Bad request to " + methodKey);
            case 404:
                return new NotFoundException("Resource not found");
            case 503:
                return new ServiceUnavailableException("Service temporarily unavailable");
            default:
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
```

---

## WireMock for Testing

**WireMock** is a powerful tool for mocking HTTP services. It's essential for testing Feign clients without depending on actual external services.

### Why WireMock?

- ✅ **Isolation**: Test your service without external dependencies
- ✅ **Speed**: No network calls, tests run faster
- ✅ **Reliability**: No flaky tests due to network issues
- ✅ **Control**: Simulate various scenarios (success, errors, timeouts)
- ✅ **Repeatability**: Consistent test results

### Setting Up WireMock

Add WireMock dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
    <scope>test</scope>
</dependency>

<!-- Or use WireMock directly -->
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

### Basic WireMock Test

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)
class OrderServiceTest {
    
    @Autowired
    private OrderService orderService;
    
    @Test
    void shouldCreateOrderWhenInventoryIsAvailable() {
        // Arrange: Mock the inventory service response
        stubFor(get(urlEqualTo("/api/inventory/IPHONE_15"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "skuCode": "IPHONE_15",
                        "quantity": 100
                    }
                    """)));
        
        // Act: Create an order
        OrderRequest request = OrderRequest.builder()
            .skuCode("IPHONE_15")
            .quantity(2)
            .build();
            
        OrderResponse response = orderService.createOrder(request);
        
        // Assert
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        
        // Verify the inventory service was called
        verify(getRequestedFor(urlEqualTo("/api/inventory/IPHONE_15")));
    }
    
    @Test
    void shouldThrowExceptionWhenInventoryServiceIsDown() {
        // Arrange: Mock service unavailable
        stubFor(get(urlEqualTo("/api/inventory/IPHONE_15"))
            .willReturn(aResponse()
                .withStatus(503)
                .withFixedDelay(1000)));
        
        // Act & Assert
        OrderRequest request = OrderRequest.builder()
            .skuCode("IPHONE_15")
            .quantity(2)
            .build();
            
        assertThrows(ServiceUnavailableException.class, () -> {
            orderService.createOrder(request);
        });
    }
}
```

### Advanced WireMock Scenarios

#### Testing Error Responses

```java
@Test
void shouldHandleInventoryNotFound() {
    stubFor(get(urlEqualTo("/api/inventory/INVALID_SKU"))
        .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                    "error": "Inventory not found",
                    "message": "SKU code INVALID_SKU does not exist"
                }
                """)));
    
    OrderRequest request = OrderRequest.builder()
        .skuCode("INVALID_SKU")
        .quantity(1)
        .build();
        
    assertThrows(NotFoundException.class, () -> {
        orderService.createOrder(request);
    });
}
```

#### Testing Network Delays

```java
@Test
void shouldHandleSlowInventoryService() {
    stubFor(get(urlEqualTo("/api/inventory/IPHONE_15"))
        .willReturn(aResponse()
            .withStatus(200)
            .withFixedDelay(3000) // 3 second delay
            .withBody("""
                {
                    "skuCode": "IPHONE_15",
                    "quantity": 100
                }
                """)));
    
    // Test timeout handling
    assertThrows(TimeoutException.class, () -> {
        orderService.createOrder(request);
    });
}
```

#### Request Matching with Parameters

```java
@Test
void shouldVerifyRequestParameters() {
    stubFor(post(urlEqualTo("/api/inventory/reserve"))
        .withRequestBody(matchingJsonPath("$.skuCode", equalTo("IPHONE_15")))
        .withRequestBody(matchingJsonPath("$.quantity", equalTo("2")))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("""
                {
                    "reserved": true,
                    "reservationId": "12345"
                }
                """)));
    
    // Test your service
    ReservationResponse response = orderService.reserveInventory("IPHONE_15", 2);
    
    assertEquals("12345", response.getReservationId());
}
```

### WireMock Architecture Diagram

![WireMock Testing Architecture](image/part4_openFeign/1765637753805.png)

### Feign Client Flow

![OpenFeign Request Flow](image/part4_openFeign/1765637652419.png)

---

## Best Practices

### 1. **Use DTOs for Request/Response**

Always create dedicated Data Transfer Objects:

```java
@Data
@Builder
public class InventoryResponse {
    private String skuCode;
    private Integer quantity;
    private boolean available;
}
```

### 2. **Implement Circuit Breaker Pattern**

Combine OpenFeign with Resilience4j:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
</dependency>
```

```java
@FeignClient(
    name = "inventory-service",
    fallback = InventoryClientFallback.class
)
public interface InventoryClient {
    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse getInventory(@PathVariable String skuCode);
}

@Component
public class InventoryClientFallback implements InventoryClient {
    @Override
    public InventoryResponse getInventory(String skuCode) {
        return InventoryResponse.builder()
            .skuCode(skuCode)
            .quantity(0)
            .available(false)
            .build();
    }
}
```

### 3. **Enable Request/Response Logging**

```yaml
logging:
  level:
    com.example.client.InventoryClient: DEBUG

feign:
  client:
    config:
      default:
        loggerLevel: FULL
```

### 4. **Configure Timeouts Appropriately**

```yaml
feign:
  client:
    config:
      inventory-service:
        connectTimeout: 5000
        readTimeout: 10000
```

### 5. **Use WireMock for All External Service Tests**

- Never depend on real services in unit/integration tests
- Create reusable WireMock stubs
- Test both success and failure scenarios

### 6. **Centralize Feign Configuration**

```java
@Configuration
public class FeignClientConfiguration {
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
    
    @Bean
    public OkHttpClient client() {
        return new OkHttpClient();
    }
}
```

### 7. **Handle Errors Gracefully**

```java
@ControllerAdvice
public class FeignExceptionHandler {
    
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException e) {
        ErrorResponse error = ErrorResponse.builder()
            .status(e.status())
            .message("Error calling external service")
            .timestamp(LocalDateTime.now())
            .build();
            
        return ResponseEntity
            .status(e.status())
            .body(error);
    }
}
```

---

## Summary

### OpenFeign Benefits

| Feature | Benefit |
|---------|---------|
| **Declarative** | Clean, readable code |
| **Type-Safe** | Compile-time checking |
| **Integration** | Works with Spring Cloud ecosystem |
| **Testing** | Easy to mock with WireMock |
| **Maintainable** | Centralized HTTP client logic |

### WireMock Benefits

| Feature | Benefit |
|---------|---------|
| **Isolation** | No external dependencies |
| **Speed** | Fast test execution |
| **Reliability** | Consistent test results |
| **Flexibility** | Simulate any scenario |
| **Control** | Test edge cases easily |

---

## Next Steps

1. ✅ Implement OpenFeign clients for all inter-service communication
2. ✅ Write comprehensive WireMock tests
3. ✅ Add circuit breaker patterns with Resilience4j
4. ✅ Configure proper timeouts and retry logic
5. ✅ Monitor Feign client metrics with Actuator

---

## Additional Resources

- [Spring Cloud OpenFeign Documentation](https://spring.io/projects/spring-cloud-openfeign)
- [WireMock Official Documentation](http://wiremock.org/)
- [Resilience4j Integration](https://resilience4j.readme.io/docs/getting-started-3)
- [Feign GitHub Repository](https://github.com/OpenFeign/feign)
