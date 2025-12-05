# Product Service with MongoDB

This project uses Docker Compose to run a MongoDB instance for the product service.

## Quick Start

### Start the Database
```sh
docker compose up -d
```

### Stop the Database
```sh
docker compose down
```

---

## Database Connections

### 1. Connect as Root User (Administrative Access)
Use this for database administration and user management:
```sh
docker exec -it mongodb mongosh -u root -p supersecretpassword
```

### 2. Connect as Application User (Recommended)
Use this for normal application operations:
```sh
docker exec -it mongodb mongosh -u product-user -p apppassword --authenticationDatabase admin
```

---

## User Management

### Current Users Setup
Your MongoDB instance comes pre-configured with:

| Username | Password | Authentication DB | Roles | Purpose |
|----------|----------|-------------------|-------|---------|
| `root` | `supersecretpassword` | `admin` | `root` | Full administrative access |
| `product-user` | `apppassword` | `admin` | `readWrite` on `product-service` | Application access |

### Create Additional Users (if needed)
```javascript
// Connect as root first, then:
use admin
db.createUser({
  user: "new-username",
  pwd: passwordPrompt(), // Secure password entry
  roles: [ 
    { role: "readWrite", db: "product-service" },
    // Add other databases as needed
  ]
})
```

### Verify Users
```javascript
// Check all users in admin database
use admin
db.getUsers()

// Check roles for specific user
db.getUser("product-user")
```

---

## Database Operations

### Basic Commands
```javascript
// List all databases
show dbs

// Switch to product-service database
use product-service

// List collections
show collections

// View all products
db.products.find()

// Insert a test product
db.products.insertOne({ 
  name: "Sample Product", 
  price: 29.99,
  category: "electronics",
  createdAt: new Date()
})

// Count documents
db.products.countDocuments()
```

### Useful Queries
```javascript
// Find products by price range
db.products.find({ price: { $gte: 20, $lte: 50 } })

// Find by name (case insensitive)
db.products.find({ name: { $regex: 'laptop', $options: 'i' } })

// Update a product
db.products.updateOne(
  { _id: ObjectId("...") },
  { $set: { price: 39.99 } }
)

// Delete a product
db.products.deleteOne({ _id: ObjectId("...") })
```

---

## Application Configuration

### Spring Boot Connection
Your application uses this configuration in `application.properties`:
```properties
spring.data.mongodb.uri=mongodb://product-user:apppassword@localhost:27017/product-service?authSource=admin
```

### Connection Details
- **Host**: `localhost`
- **Port**: `27017`
- **Database**: `product-service`
- **Authentication**: `admin` database

---

## Troubleshooting

### Common Issues

1. **Authentication Failed**
   ```sh
   # Verify user exists
   docker exec -it mongodb mongosh -u root -p supersecretpassword --eval "use admin; db.getUser('product-user')"
   ```

2. **Connection Refused**
   ```sh
   # Check if MongoDB is running
   docker ps
   # Check logs
   docker logs mongodb
   ```

3. **Database Not Found**
   ```javascript
   // The database will be created automatically when first used
   // Verify it exists:
   show dbs
   ```

### Reset Database (⚠️ Destructive)
```sh
# Stop and remove containers and volumes
docker compose down -v
# Restart
docker compose up -d
```

---

## Security Notes

- ✅ Application uses dedicated `product-user` with minimal permissions
- ✅ Root user is secured with strong password
- ✅ Authentication required for all connections
- 🔒 Passwords are stored in Docker Compose file (consider using secrets for production)

## Development Workflow

1. **Start services**: `docker compose up -d`
2. **Verify connection**: Use application user connection above
3. **Run application**: Application will connect automatically
4. **Monitor logs**: `docker logs mongodb -f`
5. **Stop services**: `docker compose down`

For production deployments, consider:
- Using MongoDB Atlas (cloud)
- Implementing proper secret management
- Setting up replica sets for high availability
- Configuring proper backup strategies

---

## Data Model Concepts

### Product vs. SKU

In e-commerce and inventory systems, it's important to understand the difference between a general "product" and a specific "SKU".

*   **Product:** This is the general item that a customer sees, like the "Classic Crewneck T-Shirt". It's a marketing concept.

*   **SKU (Stock Keeping Unit):** This is the unique identifier for a *specific variation* of a product. A single product can have many different SKUs. This is essential for tracking inventory accurately.

#### Example: T-Shirt Variations

Imagine you sell a "Classic Crewneck T-Shirt" that comes in different colors and sizes. Each unique combination must have its own SKU.

| Product          | Color | Size   | SKU             |
|------------------|-------|--------|-----------------|
| Classic Crewneck | Red   | Small  | `TSHIRT-RED-S`  |
| Classic Crewneck | Red   | Medium | `TSHIRT-RED-M`  |
| Classic Crewneck | Blue  | Small  | `TSHIRT-BLUE-S` |
| Classic Crewneck | Blue  | Medium | `TSHIRT-BLUE-M` |

When an order is placed, it references the specific `skuCode` to ensure the correct item is picked from the warehouse and the inventory for that specific variation is updated.
