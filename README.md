# SPRING_CommerceFlow-MS

⚠️ **Status**: Under Development

This repository is a learning project for Spring Boot microservices inspired by the "Spring Boot Microservices Tutorial" (Programming Techie). It contains the initial version of the Product Service and reference docs to build out the full microservices system (Order, Inventory, Notification, etc.).

---

## Project Overview

The SPRING_CommerceFlow-MS project demonstrates a microservices architecture using Spring Boot and Spring Cloud concepts. The project focuses on building small, single-responsibility services with their own data stores and provides best practices around composition, messaging, and observability.

Services target (planned):
- Product Service (implemented)
- Order Service (TBD)
- Inventory Service (TBD)
- Notification Service (TBD)

This repo is primarily a learning and teaching reference and is under active development.

---


# Document Reference : 

- Author : Ayoub majjid 
- Date :  24/11/2025
- Version : 1.0

- Course source :  [text](https://www.youtube.com/playlist?list=PLSVW22jAG8pDeU80nDzbUgr8qqzEMppi8)
- course name : Spring boot microservices tutorial
- channel name : Programming techie 

# Part 1 - Building Services

## Introduction: The Microservices Paradigm

### What are Microservices?
Microservices is an architectural style that structures an application as a collection of small, autonomous services, modeled around a business domain. 
- **Monolith vs. Microservices**: Unlike a monolithic application where all functionality (UI, business logic, database access) is bundled into a single deployable unit, microservices split these concerns.
- **Why do we need them?**: 
    - **Scalability**: You can scale specific services (e.g., the Product Service) without scaling the entire application.
    - **Agility**: Different teams can work on different services simultaneously using different technologies.
    - **Resilience**: If one service fails, it doesn't necessarily bring down the whole system.

### The Spring Boot Ecosystem
Spring Boot is the de-facto standard for building microservices in Java. It simplifies the setup of production-grade applications.
- **Spring Cloud**: While Spring Boot builds the services, **Spring Cloud** provides the tools to make them work together (Service Discovery, Configuration, Circuit Breakers, Gateway).

# Services we are going to build : 
- Product Service
- Order Service
- Inventory Service
- Notification Service
![alt text](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image.png)


## Architecture Deep Dive
![alt text](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image-1.png)

### 1. Infrastructure & Orchestration: The "Glue"
Before looking at the individual services, it's crucial to understand what holds them together.
- **Docker (Containerization)**: Each service (Product, Order, etc.) and tool (Prometheus, Grafana) is packaged into a **Docker Container**. This ensures that the application runs exactly the same on your laptop as it does on a server, bundling all dependencies (Java, libraries) together.
- **Kubernetes (Orchestration)**: This is the system that "gathers" all these components. It manages the containers, ensuring they are running, healthy, and can talk to each other. If a container crashes, Kubernetes restarts it. It acts as the operating system for your distributed application.

### 2. Component Roles & Responsibilities

#### A. The Gatekeepers (Edge Layer)
- **API Gateway**: The single entry point for all traffic. It hides the internal complexity of the system. Instead of a client calling `order-service:8081` and `product-service:8080`, they just call `api.myapp.com/order` and `api.myapp.com/product`.
- **Auth Server (Keycloak/OAuth2)**: Manages identity. It issues security tokens (JWTs) so that services know *who* is making the request and *what* they are allowed to do.

#### B. The Core Business Services
- **Product Service (MongoDB)**: Handles the catalog. Uses a NoSQL database because product attributes can vary wildly (flexible schema).
- **Order Service (MySQL)**: The central transaction handler. Uses a Relational Database (ACID compliance) because order data must be strictly consistent.
- **Inventory Service (MySQL)**: Manages stock. It is a critical dependency for the Order Service.
- **Notification Service**: A decoupled service responsible for sending emails/SMS. It doesn't need to respond immediately to the user.

#### C. The Supporting Cast (Infrastructure Services)
- **Eureka (Service Discovery)**: The "Phonebook". Services register here (e.g., "I am Order Service, IP: 10.0.0.5"). When the Gateway needs to route a request, it asks Eureka where the Order Service is.
- **Kafka (Message Broker)**: The "Post Office". Enables asynchronous communication. Services drop messages here and move on, without waiting for a receiver to pick them up.

### 3. Full Request Flow Example: "Placing an Order"
Let's trace a single user request to see how these components interact in real-time.

**Scenario**: A user clicks "Buy Now" on a generic iPhone 15.

1.  **Entry**: The request hits the **API Gateway**.
2.  **Security Check**: The Gateway checks the Authorization Header. If valid, it forwards the request to the **Order Service**.
3.  **Business Logic (Order Service)**:
    *   The Order Service receives the request.
    *   **Synchronous Call**: It *immediately* calls the **Inventory Service** (via HTTP) to ask: "Is iPhone 15 in stock?".
    *   *Wait...*: The Order Service waits for a Yes/No.
    *   If "Yes": The Order is saved to the MySQL database.
4.  **Async Notification**:
    *   The Order Service does *not* want to wait for an email to be sent (that takes too long).
    *   It sends a "OrderPlacedEvent" to **Kafka** and immediately returns "Order Successful" to the user.
5.  **Background Processing**:
    *   The **Notification Service** is listening to Kafka. It picks up the message and sends an email to the user.

### 4. Analyzing Insights (Observability)
# Document Reference : 

- Author : Ayoub majjid 
- Date :  24/11/2025
- Version : 1.0

- Course source :  [text](https://www.youtube.com/playlist?list=PLSVW22jAG8pDeU80nDzbUgr8qqzEMppi8)
- course name : Spring boot microservices tutorial
- channel name : Programming techie 

# Part 1 - Building Services

## Introduction: The Microservices Paradigm

### What are Microservices?
Microservices is an architectural style that structures an application as a collection of small, autonomous services, modeled around a business domain. 
- **Monolith vs. Microservices**: Unlike a monolithic application where all functionality (UI, business logic, database access) is bundled into a single deployable unit, microservices split these concerns.
- **Why do we need them?**: 
    - **Scalability**: You can scale specific services (e.g., the Product Service) without scaling the entire application.
    - **Agility**: Different teams can work on different services simultaneously using different technologies.
    - **Resilience**: If one service fails, it doesn't necessarily bring down the whole system.

### The Spring Boot Ecosystem
Spring Boot is the de-facto standard for building microservices in Java. It simplifies the setup of production-grade applications.
- **Spring Cloud**: While Spring Boot builds the services, **Spring Cloud** provides the tools to make them work together (Service Discovery, Configuration, Circuit Breakers, Gateway).

# Services we are going to build : 
- Product Service
- Order Service
- Inventory Service
- Notification Service
![alt text](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image.png)


## Architecture Deep Dive
![alt text](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image-1.png)

### 1. Infrastructure & Orchestration: The "Glue"
Before looking at the individual services, it's crucial to understand what holds them together.
- **Docker (Containerization)**: Each service (Product, Order, etc.) and tool (Prometheus, Grafana) is packaged into a **Docker Container**. This ensures that the application runs exactly the same on your laptop as it does on a server, bundling all dependencies (Java, libraries) together.
- **Kubernetes (Orchestration)**: This is the system that "gathers" all these components. It manages the containers, ensuring they are running, healthy, and can talk to each other. If a container crashes, Kubernetes restarts it. It acts as the operating system for your distributed application.

### 2. Component Roles & Responsibilities

#### A. The Gatekeepers (Edge Layer)
- **API Gateway**: The single entry point for all traffic. It hides the internal complexity of the system. Instead of a client calling `order-service:8081` and `product-service:8080`, they just call `api.myapp.com/order` and `api.myapp.com/product`.
- **Auth Server (Keycloak/OAuth2)**: Manages identity. It issues security tokens (JWTs) so that services know *who* is making the request and *what* they are allowed to do.

#### B. The Core Business Services
- **Product Service (MongoDB)**: Handles the catalog. Uses a NoSQL database because product attributes can vary wildly (flexible schema).
- **Order Service (MySQL)**: The central transaction handler. Uses a Relational Database (ACID compliance) because order data must be strictly consistent.
- **Inventory Service (MySQL)**: Manages stock. It is a critical dependency for the Order Service.
- **Notification Service**: A decoupled service responsible for sending emails/SMS. It doesn't need to respond immediately to the user.

#### C. The Supporting Cast (Infrastructure Services)
- **Eureka (Service Discovery)**: The "Phonebook". Services register here (e.g., "I am Order Service, IP: 10.0.0.5"). When the Gateway needs to route a request, it asks Eureka where the Order Service is.
- **Kafka (Message Broker)**: The "Post Office". Enables asynchronous communication. Services drop messages here and move on, without waiting for a receiver to pick them up.

### 3. Full Request Flow Example: "Placing an Order"
Let's trace a single user request to see how these components interact in real-time.

**Scenario**: A user clicks "Buy Now" on a generic iPhone 15.

1.  **Entry**: The request hits the **API Gateway**.
2.  **Security Check**: The Gateway checks the Authorization Header. If valid, it forwards the request to the **Order Service**.
3.  **Business Logic (Order Service)**:
    *   The Order Service receives the request.
    *   **Synchronous Call**: It *immediately* calls the **Inventory Service** (via HTTP) to ask: "Is iPhone 15 in stock?".
    *   *Wait...*: The Order Service waits for a Yes/No.
    *   If "Yes": The Order is saved to the MySQL database.
4.  **Async Notification**:
    *   The Order Service does *not* want to wait for an email to be sent (that takes too long).
    *   It sends a "OrderPlacedEvent" to **Kafka** and immediately returns "Order Successful" to the user.
5.  **Background Processing**:
    *   The **Notification Service** is listening to Kafka. It picks up the message and sends an email to the user.

### 4. Analyzing Insights (Observability)
How do we know all step 3 happened correctly?
-   **Distributed Tracing (Tempo/Zipkin)**: A unique "Trace ID" is attached to the request at the Gateway. This ID travels with the request to Order -> Inventory -> Kafka -> Notification. We can view a timeline graph showing exactly how long each step took.
-   **Metrics (Prometheus/Grafana)**: We can see graphs showing "Orders per second" or "Inventory Service Latency".
-   **Logs (Loki)**: If the Inventory check failed, we can search the logs for that specific Trace ID and see the error message from the Inventory Service.


# Internal Service Architecture: The Layered Approach

![Service Architecture Diagram](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image/part1_building_services/1763986131224.png)

Each microservice in our system follows a standard **Layered Architecture**. This separation of concerns ensures that the code is maintainable, testable, and easy to understand.

### 1. The Presentation Layer: Controller
*   **Role**: The "Front Desk". It is the entry point for HTTP requests (GET, POST, PUT, DELETE).
*   **Responsibilities**:
    *   **Handling Requests**: Maps URLs (e.g., `/api/product`) to Java methods.
    *   **Validation**: Checks if the incoming data is valid (e.g., "Price cannot be negative").
    *   **DTOs (Data Transfer Objects)**: Converts the raw JSON request into a Java object. It ensures we don't expose our internal database entities directly to the outside world.
*   **Interaction**: It passes the clean, validated data to the **Service Layer**. It *never* talks to the database directly.

### 2. The Business Logic Layer: Service
*   **Role**: The "Brain". This is where the actual work happens.
*   **Responsibilities**:
    *   **Business Rules**: Implements logic like "Check if user has enough credit" or "Calculate total price with tax".
    *   **Orchestration**: It coordinates between different components. It might call the Repository to save data, then call the Message Queue to send a notification.
    *   **Transactions**: Ensures that a series of operations either all succeed or all fail (Atomic).
*   **Interaction**: 
    *   Calls the **Repository** to fetch/save data.
    *   Sends messages to the **Message Queue** (Producer).

### 3. The Data Access Layer: Repository
*   **Role**: The "Librarian". It abstracts the underlying database technology.
*   **Responsibilities**:
    *   **CRUD Operations**: Provides methods to Create, Read, Update, and Delete records.
    *   **Query Abstraction**: In Spring Data, we can simply define an interface (e.g., `findBySkuCode`), and Spring automatically generates the SQL query for us.
*   **Interaction**: Talks directly to the **Database** (MySQL/MongoDB).

### 4. External Dependencies (Outside the Service)
These components live outside the application code but are critical for its operation.

*   **Database (Data Storage)**:
    *   Stores the persistent state of the service.
    *   *Example*: The Product Service stores product details in MongoDB; the Order Service stores orders in MySQL.
    *   **Isolation**: Each service owns its own database. The Order Service cannot read the Product Service's database directly; it must ask the Product Service via API.

*   **Message Queue (Asynchronous Communication)**:
    *   Acts as a buffer and distributor for messages.
    *   *Example*: When an order is placed, the Order Service drops a message here. The Notification Service picks it up later. This ensures that if the Notification Service is down, the Order Service doesn't crash—the message just waits in the queue.


# Step-by-Step: Setting up the Product Service

![Spring Initializr Setup](docs/Spring%20Boot%20Microservices%20Tutorial%20-%20Part%201%20-%20Building%20Services.md/image/part1_building_services/1763986715951.png)

To bootstrap our **Product Service**, we use **Spring Initializr** (start.spring.io). This tool generates a production-ready project structure with all the necessary build configurations.

### 1. Project Metadata
*   **Project**: Maven (Dependency Management tool)
*   **Language**: Java
*   **Spring Boot**: 3.x.x (Latest stable version)
*   **Group**: `com.majjid.microservices` (Your organization ID)
*   **Artifact**: `product-service` (The name of the jar file)

### 2. Key Dependencies & Why We Need Them

We selected four specific dependencies. Here is the reasoning for each:

*   **Spring Web**:
    *   **Why?**: This is the core dependency for building RESTful APIs.
    *   **What it does**: It includes **Tomcat** (an embedded web server) so we can run our app as a standalone JAR. It also provides the annotations we need like `@RestController`, `@GetMapping`, and `@PostMapping` to define our API endpoints.

*   **Spring Data MongoDB**:
    *   **Why?**: The Product Service needs to store product data (Name, Description, Price). We chose MongoDB (NoSQL) because product data can be unstructured or variable.
    *   **What it does**: It provides the `MongoRepository` interface, allowing us to interact with the database using simple Java methods (e.g., `repository.save(product)`) instead of writing raw database queries.

*   **Lombok**:
    *   **Why?**: Java can be verbose. We don't want to write hundreds of lines of Getters, Setters, and Constructors.
    *   **What it does**: It's a library that automatically plugs into your editor and build tool. By adding annotations like `@Data`, `@AllArgsConstructor`, and `@Builder` to our classes, Lombok generates all that boilerplate code for us at compile time.

*   **Testcontainers**:
    *   **Why?**: For reliable integration testing.
    *   **What it does**: It allows us to spin up a real MongoDB instance inside a Docker container during our tests. This ensures that our tests run against a real database environment rather than an in-memory mock, preventing "it works on my machine" issues.
    - 


# create a docker compose file for the product service : 

```yaml
services:
  mongodb:
    image: mongo:latest
    container_name: mongodb
    ports:
      - "27017:27017"
    environment:
      # Still create a root user for administrative tasks
      - MONGO_INITDB_ROOT_USERNAME=root # Admin user
      - MONGO_INITDB_ROOT_PASSWORD=supersecretpassword # Use a strong password
      # Create a dedicated user for the application
      - MONGO_INITDB_DATABASE=product-service # The database for the new user
      - MONGO_INITDB_USERNAME=product-user # Application-specific user
      - MONGO_INITDB_PASSWORD=apppassword # Application-specific password
    volumes:
      - mongodb_data:/data/db

#  product-service:
#    build: .
#    image: product-service
#    container_name: product-service
#    ports:
#      - 8080:8080
#
volumes:
  mongodb_data:

```

## License

This repository is provided for learning and demonstration purposes. No license specified — please contact the repository owner for usage permissions.
