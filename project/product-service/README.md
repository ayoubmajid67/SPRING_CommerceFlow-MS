# Product Service - Code Analysis & Architecture Documentation

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture & Design Patterns](#architecture--design-patterns)
3. [MongoDB Integration](#mongodb-integration)
4. [File-by-File Analysis](#file-by-file-analysis)
5. [Key Components Deep Dive](#key-components-deep-dive)
6. [API Endpoints Reference](#api-endpoints-reference)
7. [NoSQL Design Patterns](#nosql-design-patterns)
8. [Best Practices Implemented](#best-practices-implemented)
9. [Areas for Improvement](#areas-for-improvement)

---

## Project Overview

The **Product Service** is a microservice responsible for managing the product catalog in an e-commerce system. It uses **MongoDB** (NoSQL) for flexible schema design, allowing products with varying attributes without rigid table structures.

### Technology Stack

- **Spring Boot 4.0.0** - Latest Spring Boot framework
- **Spring Data MongoDB** - MongoDB integration and repository support
- **MongoDB** - NoSQL document database
- **MapStruct 1.5.5** - Type-safe DTO mapping
- **Lombok** - Reduces boilerplate code
- **SpringDoc OpenAPI 2.3.0** - API documentation (Swagger UI)
- **Spring Actuator** - Health checks and monitoring
- **Testcontainers** - Integration testing with real MongoDB
- **REST Assured 5.3.2** - API testing framework

### Service Responsibilities

✅ **Product Catalog Management**
- Create new products
- Retrieve product details (single or all)
- Update existing products
- Delete products

✅ **Flexible Schema**
- NoSQL allows products with different attributes
- No rigid table structure
- Easy to add new product types

✅ **RESTful API**
- Standard CRUD operations
- JSON request/response format
- Proper HTTP status codes

---

## Architecture & Design Patterns

### 1. **Layered Architecture**

```
┌─────────────────────────────────────┐
│      Controller Layer               │  ← REST API Endpoints
├─────────────────────────────────────┤
│      Service Layer                  │  ← Business Logic
├─────────────────────────────────────┤
│      Repository Layer               │  ← Data Access (MongoDB)
├─────────────────────────────────────┤
│      Database (MongoDB)             │  ← Document Storage
└─────────────────────────────────────┘
```

**Key Difference from SQL Services:**
```
SQL (Inventory/Order)          NoSQL (Product)
├── JPA Entity                 ├── MongoDB Document
├── Table Rows                 ├── JSON Documents
├── Fixed Schema               ├── Flexible Schema
└── Relational Joins           └── Embedded Documents
```

### 2. **Design Patterns Used**

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **DTO Pattern** | `ProductRequestDto`, `ProductResponseDto` | Separate API contracts from domain models |
| **Repository Pattern** | `ProductRepository extends MongoRepository` | Abstract MongoDB access |
| **Mapper Pattern** | `ProductMapper` with MapStruct | Type-safe entity ↔ DTO conversion |
| **Service Layer Pattern** | `ProductService` implements `IProductService` | Encapsulate business logic |
| **Builder Pattern** | `@Builder` on Product entity | Fluent object creation |
| **Document Pattern** | `@Document` annotation | MongoDB collection mapping |

---

## MongoDB Integration

### **Why MongoDB for Product Service?**

1. **Flexible Schema** 📦
   ```json
   // Product 1: Simple T-Shirt
   {
     "id": "1",
     "name": "Classic T-Shirt",
     "price": 19.99,
     "description": "Cotton t-shirt"
   }
   
   // Product 2: Laptop with extra attributes
   {
     "id": "2",
     "name": "Dell XPS 15",
     "price": 1499.99,
     "description": "High-performance laptop",
     "specs": {
       "cpu": "Intel i7",
       "ram": "16GB",
       "storage": "512GB SSD"
     },
     "warranty": "2 years"
   }
   ```
   ✅ Both stored in same collection without schema changes!

2. **Fast Read Performance** ⚡
   - No JOINs needed
   - Documents stored together
   - Perfect for product catalogs

3. **Horizontal Scalability** 📈
   - Easy to shard across servers
   - Handles millions of products

### **MongoDB vs MySQL Comparison**

| Feature | MongoDB (Product) | MySQL (Inventory/Order) |
|---------|------------------|------------------------|
| **Schema** | Flexible, dynamic | Fixed, rigid |
| **Data Model** | Documents (JSON) | Tables (rows/columns) |
| **Relationships** | Embedded or referenced | Foreign keys + JOINs |
| **Scalability** | Horizontal (sharding) | Vertical (more powerful server) |
| **Best For** | Catalogs, content, logs | Transactions, financial data |
| **ACID** | Document-level | Full ACID compliance |

---

## File-by-File Analysis

### 📁 **Project Structure**

```
product/
├── ProductServiceApplication.java    # Main entry point
├── config/
│   ├── CustomAppException.java       # Custom exception class
│   └── GlobalExceptionHandler.java   # Centralized exception handling
├── controller/
│   └── ProductController.java        # REST API endpoints
├── Dto/
│   ├── ResponseDto.java              # Generic response wrapper
│   ├── ProductRequestDto.java        # Create/Update request
│   └── ProductResponseDto.java       # Product response
├── mappers/
│   ├── CustomMapper.java             # Generic mapper interface
│   └── ProductMapper.java            # MapStruct mapper
├── model/
│   └── Product.java                  # MongoDB document
├── repository/
│   └── ProductRepository.java        # MongoDB repository
└── service/
    └── ProductService/
        ├── IProductService.java      # Service interface
        └── ProductService.java       # Service implementation
```

---

## Key Components Deep Dive

### 1. **ProductServiceApplication.java** - Entry Point

```java
@SpringBootApplication
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

**Analysis:**
- ✅ Clean, minimal configuration
- ✅ No extra beans or configurations needed
- 📝 **Note**: MongoDB auto-configuration handled by Spring Boot

**Application Properties:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://product-user:apppassword@localhost:27017/product-service?authSource=admin
      # Or separate properties:
      host: localhost
      port: 27017
      database: product-service
      username: product-user
      password: apppassword
      authentication-database: admin
```

---

### 2. **Product.java** - MongoDB Document

```java
@Document(value = "product")  // ← MongoDB collection name
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id  // ← MongoDB ObjectId
    private String id;
    
    private String name;
    private String description;
    private BigDecimal price;
}
```

**Analysis:**

✅ **Strengths:**
- Uses `@Document` for MongoDB mapping
- `@Id` with `String` type (MongoDB ObjectId)
- `BigDecimal` for price (correct for money)
- `@Builder` for fluent creation
- Clean, simple structure

⚠️ **Missing Features:**

1. **No Validation Constraints**
   ```java
   // ❌ Current: No validation
   private String name;
   
   // ✅ Should be:
   @NotBlank(message = "Product name is required")
   @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
   private String name;
   
   @NotNull(message = "Price is required")
   @DecimalMin(value = "0.01", message = "Price must be greater than 0")
   private BigDecimal price;
   ```

2. **No Indexing**
   ```java
   // Missing indexes for search performance
   @Indexed(unique = true)
   private String sku;  // Product SKU
   
   @Indexed
   private String name;  // For name searches
   
   @Indexed
   private String category;  // For filtering
   ```

3. **No Audit Fields**
   ```java
   // Missing:
   @CreatedDate
   private LocalDateTime createdAt;
   
   @LastModifiedDate
   private LocalDateTime updatedAt;
   ```

4. **No Soft Delete Support**
   ```java
   // Missing:
   private Boolean deleted = false;
   private LocalDateTime deletedAt;
   ```

**Improved Version:**

```java
@Document(value = "product")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    
    @Id
    private String id;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100)
    @Indexed
    private String name;
    
    @Size(max = 500, message = "Description too long")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    private BigDecimal price;
    
    @Indexed(unique = true)
    private String sku;  // Stock Keeping Unit
    
    @Indexed
    private String category;
    
    private Integer stockQuantity;
    
    private List<String> images;  // Product image URLs
    
    private Map<String, Object> attributes;  // Flexible attributes
    
    @Builder.Default
    private Boolean deleted = false;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

---

### 3. **ProductRepository.java** - Data Access Layer

```java
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // Empty - inherits all CRUD methods from MongoRepository
}
```

**Analysis:**

✅ **Good:**
- Extends `MongoRepository<Product, String>`
- Inherits 20+ methods: `save()`, `findAll()`, `findById()`, `deleteById()`, etc.
- Type-safe with generics

⚠️ **Missing Useful Queries:**

```java
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    // 1. Find by name (case-insensitive)
    List<Product> findByNameContainingIgnoreCase(String name);
    
    // 2. Find by price range
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    // 3. Find by category
    List<Product> findByCategory(String category);
    
    // 4. Find by SKU
    Optional<Product> findBySku(String sku);
    
    // 5. Check if product exists by name
    boolean existsByName(String name);
    
    // 6. Custom query with @Query annotation
    @Query("{ 'price': { $gte: ?0, $lte: ?1 } }")
    List<Product> findProductsInPriceRange(BigDecimal min, BigDecimal max);
    
    // 7. Find active products (soft delete)
    List<Product> findByDeletedFalse();
    
    // 8. Aggregation example
    @Aggregation(pipeline = {
        "{ $group: { _id: '$category', count: { $sum: 1 } } }"
    })
    List<CategoryCount> countByCategory();
}
```

---

### 4. **ProductService.java** - Business Logic

Let's analyze each method:

#### **A. Create Product**

```java
@Override
public ResponseDto<ProductResponseDto> createProduct(ProductRequestDto productRequestDto) {
    log.info("Creating new product: {}", productRequestDto.name());
    
    Product product = mapper.toObject(productRequestDto);
    product = productRepository.save(product);
    log.info("Product {} is saved", product.getId());
    
    ProductResponseDto responseDto = mapper.toDto(product);
    return ResponseDto.created(responseDto, "product");
}
```

**Analysis:**

✅ **Good:**
- Proper logging
- Clean mapper usage
- Returns appropriate response

💡 **Suggestions:**
1. **Add Duplicate Check**
   ```java
   // Check if product with same name already exists
   if (productRepository.existsByName(productRequestDto.name())) {
       throw new CustomAppException(HttpStatus.CONFLICT, 
           "Product with name '" + productRequestDto.name() + "' already exists");
   }
   ```

2. **Generate SKU Automatically**
   ```java
   product.setSku(generateSku(productRequestDto.name()));
   ```

---

#### **B. Get All Products**

```java
@Override
public ResponseDto<List<ProductResponseDto>> getProducts() {
    log.info("Fetching all products");
    
    List<ProductResponseDto> products = productRepository.findAll()
        .stream()
        .map(mapper::toDto)
        .toList();
    
    return ResponseDto.listed(products, "products");
}
```

**Analysis:**

✅ **Good:**
- Clean stream processing
- Proper DTO mapping

⚠️ **Issues:**

1. **No Pagination** ❌
   ```java
   // Problem: Returns ALL products (could be millions!)
   // Solution: Add pagination
   
   public ResponseDto<Page<ProductResponseDto>> getProducts(Pageable pageable) {
       Page<Product> productPage = productRepository.findAll(pageable);
       Page<ProductResponseDto> dtoPage = productPage.map(mapper::toDto);
       return ResponseDto.listed(dtoPage, "products");
   }
   ```

2. **No Filtering/Sorting**
   ```java
   // Add query parameters:
   public ResponseDto<List<ProductResponseDto>> getProducts(
       String category,
       BigDecimal minPrice,
       BigDecimal maxPrice,
       String sortBy
   ) {
       // Filter and sort logic
   }
   ```

---

#### **C. Update Product**

```java
@Override
public ResponseDto<ProductResponseDto> updateProduct(
    String productId, 
    ProductRequestDto productRequestDto
) {
    log.info("Updating product with ID: {}", productId);
    
    Product existingProduct = productRepository.findById(productId)
        .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, 
            "The Product with id = " + productId + " is Not Found"));
    
    // Update the existing product with new data
    Product updatedProduct = mapper.toObject(productRequestDto);
    updatedProduct.setId(existingProduct.getId()); // Preserve the ID
    
    updatedProduct = productRepository.save(updatedProduct);
    log.info("Product {} updated successfully", productId);
    
    ProductResponseDto responseDto = mapper.toDto(updatedProduct);
    return ResponseDto.updated(responseDto, "product");
}
```

**Analysis:**

✅ **Good:**
- Checks if product exists
- Preserves ID

⚠️ **Issue:**

**Replaces Entire Document** instead of updating fields:
```java
// ❌ Current: Creates new object, loses unmapped fields
Product updatedProduct = mapper.toObject(productRequestDto);
updatedProduct.setId(existingProduct.getId());

// ✅ Better: Update existing object
mapper.updateEntity(productRequestDto, existingProduct);
productRepository.save(existingProduct);
```

**Recommended Approach:**

```java
@Override
public ResponseDto<ProductResponseDto> updateProduct(
    String productId, 
    ProductRequestDto productRequestDto
) {
    Product existingProduct = productRepository.findById(productId)
        .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, 
            "Product not found: " + productId));
    
    // Update only provided fields
    if (productRequestDto.name() != null) {
        existingProduct.setName(productRequestDto.name());
    }
    if (productRequestDto.description() != null) {
        existingProduct.setDescription(productRequestDto.description());
    }
    if (productRequestDto.price() != null) {
        existingProduct.setPrice(productRequestDto.price());
    }
    
    Product savedProduct = productRepository.save(existingProduct);
    return ResponseDto.updated(mapper.toDto(savedProduct), "product");
}
```

---

#### **D. Delete Product**

```java
@Override
public ResponseDto<Boolean> deleteProduct(String productId) {
    log.info("Deleting product with ID: {}", productId);
    
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, 
            CustomAppException.buildNotFoundMsg(productId, OBJECT_TYPE)));
    
    productRepository.deleteById(productId);
    log.info("Product {} deleted successfully", productId);
    
    return ResponseDto.deleted(true, "product");
}
```

**Analysis:**

✅ **Good:**
- Validates existence before deleting
- Proper logging

💡 **Suggestion: Implement Soft Delete**

```java
@Override
public ResponseDto<Boolean> deleteProduct(String productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, 
            "Product not found: " + productId));
    
    // Soft delete instead of hard delete
    product.setDeleted(true);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
    
    log.info("Product {} soft deleted", productId);
    return ResponseDto.deleted(true, "product");
}

// Add method to permanently delete
@Override
public ResponseDto<Boolean> permanentlyDeleteProduct(String productId) {
    productRepository.deleteById(productId);
    return ResponseDto.deleted(true, "product");
}
```

---

### 5. **ProductController.java** - REST API

```java
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final IProductService productService;
    
    @PostMapping
    public ResponseEntity<ResponseDto<ProductResponseDto>> createProduct(
        @Valid @RequestBody ProductRequestDto productRequestDto
    ) {
        ResponseDto<ProductResponseDto> response = productService.createProduct(productRequestDto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping
    public ResponseEntity<ResponseDto<List<ProductResponseDto>>> getProducts() {
        ResponseDto<List<ProductResponseDto>> response = productService.getProducts();
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> getProduct(
        @PathVariable String productId
    ) {
        ResponseDto<ProductResponseDto> response = productService.getProduct(productId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @PutMapping("/{productId}")
    public ResponseEntity<ResponseDto<ProductResponseDto>> updateProduct(
        @PathVariable String productId,
        @Valid @RequestBody ProductRequestDto productRequestDto
    ) {
        ResponseDto<ProductResponseDto> response = productService.updateProduct(productId, productRequestDto);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<Boolean>> deleteProduct(
        @PathVariable String productId
    ) {
        ResponseDto<Boolean> response = productService.deleteProduct(productId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}
```

**Analysis:**

✅ **Excellent:**
- Clean REST API design
- Proper HTTP methods (POST, GET, PUT, DELETE)
- Validation with `@Valid`
- Consistent response structure
- Path variables for resource identification

💡 **Suggestions:**

1. **Add API Documentation**
   ```java
   @Tag(name = "Product Management", description = "APIs for managing products")
   @RestController
   @RequestMapping("/products")
   public class ProductController {
       
       @Operation(summary = "Create a new product")
       @ApiResponses({
           @ApiResponse(responseCode = "201", description = "Product created"),
           @ApiResponse(responseCode = "400", description = "Invalid request"),
           @ApiResponse(responseCode = "409", description = "Product already exists")
       })
       @PostMapping
       public ResponseEntity<ResponseDto<ProductResponseDto>> createProduct(...)
   }
   ```

2. **Add Search/Filter Endpoints**
   ```java
   @GetMapping("/search")
   public ResponseEntity<ResponseDto<List<ProductResponseDto>>> searchProducts(
       @RequestParam(required = false) String name,
       @RequestParam(required = false) String category,
       @RequestParam(required = false) BigDecimal minPrice,
       @RequestParam(required = false) BigDecimal maxPrice
   ) {
       // Search logic
   }
   ```

3. **Add Pagination**
   ```java
   @GetMapping
   public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getProducts(
       @RequestParam(defaultValue = "0") int page,
       @RequestParam(defaultValue = "20") int size,
       @RequestParam(defaultValue = "name") String sortBy
   ) {
       // Pagination logic
   }
   ```

---

## API Endpoints Reference

### **Base URL:** `http://localhost:8080/products`

| Method | Endpoint | Description | Request Body | Response |
|--------|----------|-------------|--------------|----------|
| `POST` | `/` | Create product | `ProductRequestDto` | `ProductResponseDto` |
| `GET` | `/` | Get all products | - | `List<ProductResponseDto>` |
| `GET` | `/{id}` | Get product by ID | - | `ProductResponseDto` |
| `PUT` | `/{id}` | Update product | `ProductRequestDto` | `ProductResponseDto` |
| `DELETE` | `/{id}` | Delete product | - | `Boolean` |

### **Example Requests:**

#### 1. Create Product
```bash
POST /products
Content-Type: application/json

{
  "name": "iPhone 15 Pro",
  "description": "Latest Apple smartphone with A17 chip",
  "price": 999.99
}
```

**Response (201 Created):**
```json
{
  "status": 201,
  "message": "Product created successfully",
  "data": {
    "id": "65a1b2c3d4e5f6g7h8i9j0k1",
    "name": "iPhone 15 Pro",
    "description": "Latest Apple smartphone with A17 chip",
    "price": 999.99
  },
  "success": true
}
```

#### 2. Get All Products
```bash
GET /products
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Products retrieved successfully",
  "data": [
    {
      "id": "65a1b2c3d4e5f6g7h8i9j0k1",
      "name": "iPhone 15 Pro",
      "description": "Latest Apple smartphone",
      "price": 999.99
    },
    {
      "id": "65a1b2c3d4e5f6g7h8i9j0k2",
      "name": "MacBook Pro",
      "description": "Professional laptop",
      "price": 2499.99
    }
  ],
  "success": true
}
```

#### 3. Update Product
```bash
PUT /products/65a1b2c3d4e5f6g7h8i9j0k1
Content-Type: application/json

{
  "name": "iPhone 15 Pro Max",
  "description": "Larger screen version",
  "price": 1199.99
}
```

#### 4. Delete Product
```bash
DELETE /products/65a1b2c3d4e5f6g7h8i9j0k1
```

**Response (200 OK):**
```json
{
  "status": 200,
  "message": "Product deleted successfully",
  "data": true,
  "success": true
}
```

---

## NoSQL Design Patterns

### **1. Embedded Documents Pattern**

```java
@Document
public class Product {
    @Id
    private String id;
    private String name;
    
    // Embed related data instead of foreign keys
    private Specifications specs;
    private List<Review> reviews;
    private Vendor vendor;
}

@Data
class Specifications {
    private String cpu;
    private String ram;
    private String storage;
}

@Data
class Review {
    private String userId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
```

**Benefits:**
- ✅ Single query to get all data
- ✅ No JOINs needed
- ✅ Better read performance

**When to Use:**
- Data is frequently accessed together
- Data doesn't change often
- Embedded data is small

---

### **2. Reference Pattern**

```java
@Document
public class Product {
    @Id
    private String id;
    private String name;
    
    // Reference to another collection
    @DBRef
    private Category category;
    
    // Or just store the ID
    private String categoryId;
}
```

**When to Use:**
- Referenced data is large
- Referenced data changes frequently
- Need to query referenced data independently

---

### **3. Attribute Pattern** (Flexible Schema)

```java
@Document
public class Product {
    @Id
    private String id;
    private String name;
    private BigDecimal price;
    
    // Flexible attributes for different product types
    private Map<String, Object> attributes;
}

// Example documents:
// T-Shirt:
{
  "name": "Classic T-Shirt",
  "attributes": {
    "size": "M",
    "color": "Blue",
    "material": "Cotton"
  }
}

// Laptop:
{
  "name": "Dell XPS",
  "attributes": {
    "cpu": "Intel i7",
    "ram": "16GB",
    "screen": "15.6 inch"
  }
}
```

---

## Best Practices Implemented

### ✅ **1. DTO Pattern**
```java
// Separate API contracts from domain models
ProductRequestDto → Product → ProductResponseDto
```

### ✅ **2. Lombok for Boilerplate Reduction**
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
```

### ✅ **3. MapStruct for Type-Safe Mapping**
```java
@Mapper(componentModel = "spring")
public interface ProductMapper extends CustomMapper<Product, ProductRequestDto, ProductResponseDto> {
    // Compile-time generated code
}
```

### ✅ **4. Logging**
```java
@Slf4j
log.info("Creating new product: {}", productRequestDto.name());
```

### ✅ **5. Exception Handling**
```java
.orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, "Product not found"));
```

### ✅ **6. Validation**
```java
@Valid @RequestBody ProductRequestDto productRequestDto
```

### ✅ **7. RESTful API Design**
- Proper HTTP methods
- Resource-based URLs
- Appropriate status codes

### ✅ **8. Testing Infrastructure**
- Testcontainers for MongoDB
- REST Assured for API testing

---

## Areas for Improvement

### 1. **Add Pagination** ⚠️ Critical

**Problem:** `getProducts()` returns ALL products

**Solution:**
```java
@GetMapping
public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getProducts(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "name") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort.Direction sortDirection = Sort.Direction.fromString(direction);
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    
    Page<Product> productPage = productRepository.findAll(pageable);
    Page<ProductResponseDto> dtoPage = productPage.map(mapper::toDto);
    
    return ResponseEntity.ok(ResponseDto.listed(dtoPage, "products"));
}
```

### 2. **Add Indexing**

```java
@Document(value = "product")
@CompoundIndex(name = "name_category_idx", def = "{'name': 1, 'category': 1}")
public class Product {
    
    @Indexed(unique = true)
    private String sku;
    
    @Indexed
    @TextIndexed  // For full-text search
    private String name;
    
    @Indexed
    private String category;
}
```

### 3. **Add Search Functionality**

```java
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    // Full-text search
    @Query("{ $text: { $search: ?0 } }")
    List<Product> searchByText(String searchTerm);
    
    // Complex search
    @Query("{ $and: [ " +
           "{ 'category': ?0 }, " +
           "{ 'price': { $gte: ?1, $lte: ?2 } } " +
           "] }")
    List<Product> findByCategoryAndPriceRange(
        String category, 
        BigDecimal minPrice, 
        BigDecimal maxPrice
    );
}
```

### 4. **Add Caching**

```java
@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "products")
public class ProductService {
    
    @Cacheable(key = "#productId")
    public ResponseDto<ProductResponseDto> getProduct(String productId) {
        // ... existing logic
    }
    
    @CacheEvict(key = "#productId")
    public ResponseDto<ProductResponseDto> updateProduct(String productId, ...) {
        // ... existing logic
    }
    
    @CacheEvict(allEntries = true)
    public ResponseDto<ProductResponseDto> createProduct(...) {
        // ... existing logic
    }
}
```

### 5. **Add Validation Constraints**

```java
public record ProductRequestDto(
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    String name,
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    BigDecimal price
) {}
```

### 6. **Add Soft Delete**

```java
@Document
public class Product {
    // ... existing fields
    
    @Builder.Default
    private Boolean deleted = false;
    
    private LocalDateTime deletedAt;
}

// In repository:
List<Product> findByDeletedFalse();  // Get active products only

// In service:
public ResponseDto<Boolean> deleteProduct(String productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new CustomAppException(HttpStatus.NOT_FOUND, "Product not found"));
    
    product.setDeleted(true);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
    
    return ResponseDto.deleted(true, "product");
}
```

### 7. **Add Audit Fields**

```java
@Document
@EntityListeners(AuditingEntityListener.class)
public class Product {
    // ... existing fields
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
}

// Enable auditing in main class:
@SpringBootApplication
@EnableMongoAuditing
public class ProductServiceApplication {
    // ...
}
```

### 8. **Add Metrics**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    
    private final MeterRegistry meterRegistry;
    
    @Timed(value = "product.create", description = "Time to create product")
    @Counted(value = "product.create.count", description = "Number of products created")
    public ResponseDto<ProductResponseDto> createProduct(ProductRequestDto dto) {
        // ... existing logic
        
        meterRegistry.counter("products.total").increment();
    }
}
```

### 9. **Add Bulk Operations**

```java
@PostMapping("/bulk")
public ResponseEntity<ResponseDto<List<ProductResponseDto>>> createBulkProducts(
    @Valid @RequestBody List<ProductRequestDto> products
) {
    List<Product> productEntities = products.stream()
        .map(mapper::toObject)
        .toList();
    
    List<Product> savedProducts = productRepository.saveAll(productEntities);
    
    List<ProductResponseDto> response = savedProducts.stream()
        .map(mapper::toDto)
        .toList();
    
    return ResponseEntity.ok(ResponseDto.created(response, "products"));
}
```

### 10. **Add Product Categories**

```java
@Document
public class Product {
    // ... existing fields
    
    @Indexed
    private String category;
    
    private List<String> tags;  // For filtering
}

// Add category management:
@GetMapping("/categories")
public ResponseEntity<List<String>> getCategories() {
    List<String> categories = productRepository.findAll()
        .stream()
        .map(Product::getCategory)
        .distinct()
        .sorted()
        .toList();
    
    return ResponseEntity.ok(categories);
}
```

---

## Summary

### **Strengths** 🌟

1. ✅ **Clean Architecture** - Well-separated layers
2. ✅ **MongoDB Integration** - Proper use of NoSQL
3. ✅ **RESTful API** - Standard CRUD operations
4. ✅ **DTO Pattern** - Clean separation of concerns
5. ✅ **MapStruct** - Type-safe mapping
6. ✅ **Logging** - Comprehensive logging throughout
7. ✅ **Testing Infrastructure** - Testcontainers ready

### **Critical Issues** ⚠️

1. ❌ **No Pagination** - Returns all products (scalability issue)
2. ❌ **No Indexing** - Slow queries on large datasets
3. ❌ **No Validation** - Missing field constraints
4. ❌ **Update Replaces Entire Document** - Loses unmapped fields

### **What Makes This Code Professional**

- **NoSQL Best Practices**: Proper use of MongoDB documents
- **Clean Code**: Readable, maintainable structure
- **Separation of Concerns**: Each layer has clear responsibility
- **Type Safety**: MapStruct for compile-time checking
- **Modern Stack**: Spring Boot 4.0, latest dependencies

### **Key Differences from SQL Services**

| Aspect | Product (MongoDB) | Inventory/Order (MySQL) |
|--------|------------------|------------------------|
| **Schema** | Flexible | Fixed |
| **Transactions** | Document-level | Full ACID |
| **Relationships** | Embedded/Referenced | Foreign keys |
| **Scalability** | Horizontal | Vertical |
| **Best For** | Catalogs, content | Transactions, financial |

---

## Recommended Next Steps

1. ✅ Add pagination to `getProducts()`
2. ✅ Add indexes on frequently queried fields
3. ✅ Implement validation constraints
4. ✅ Add search functionality
5. ✅ Implement soft delete
6. ✅ Add audit fields
7. ✅ Add caching layer
8. ✅ Implement bulk operations
9. ✅ Add category management
10. ✅ Add API documentation (Swagger)

---

## Quick Start

### **Start MongoDB Database**
```bash
docker-compose up -d
```

### **Connect to MongoDB**
```bash
# As root
docker exec -it mongodb mongosh -u root -p supersecretpassword

# As application user
docker exec -it mongodb mongosh -u product-user -p apppassword --authenticationDatabase admin
```

### **Run Application**
```bash
./mvnw spring-boot:run
```

### **Access Swagger UI**
```
http://localhost:8080/swagger-ui.html
```

### **Health Check**
```
http://localhost:8080/actuator/health
```

---

**Excellent work on building a clean Product Service!** 🚀

The MongoDB integration is well-done, and the code structure is professional. Focus on adding pagination, indexing, and validation, and this service will be production-ready! The flexible schema design is perfect for a product catalog. 💪