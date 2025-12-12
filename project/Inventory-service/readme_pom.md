{"id":"54124","variant":"standard","title":"README_POM"}
        # Order Service – POM Overview

        This document explains the `pom.xml` configuration of the **Order Service** microservice. This service handles all order-related transactions, requiring strict ACID compliance.

        ---

        ## 1. **Project Metadata**
        - **GroupId / ArtifactId / Version**: Identifies the Maven project.
        - **Parent**: `spring-boot-starter-parent` provides default Spring Boot plugin management and dependency versions.
        - **Java Version**: 21
        - **Description**: Central transaction handler using a relational database.

        ---

        ## 2. **Properties**
        ```xml
<properties>
    <java.version>21</java.version>
    <mapstruct.version>1.5.5.Final</mapstruct.version>
    <restassured.version>5.3.2</restassured.version>
    <springdoc.version>2.3.0</springdoc.version>
</properties>
        ```
        - These properties allow centralized management of library versions.
        - Used in dependencies via `${property.name}` for easy upgrades.

        ---

        ## 3. **Dependencies**

        ### **Spring Boot Core & Web**
        - `spring-boot-starter-webmvc` – For building RESTful APIs with Spring MVC.
        - `spring-boot-starter-actuator` – Provides health checks, metrics, and monitoring endpoints.

        ### **Database & ORM**
        - `spring-boot-starter-data-jpa` – Integrates JPA and Hibernate for database access.
        - `spring-data-jpa` – Explicit JPA version control.
        - `mysql-connector-j` – JDBC driver for MySQL.
        - `spring-boot-starter-flyway` – Enables Flyway migrations for database version control.
        - `flyway-mysql` – Flyway extension for MySQL databases.

        ### **Validation**
        - `jakarta.validation-api` – Java Bean Validation API (JSR 380).
        - `spring-boot-starter-validation` – Integrates Hibernate Validator with Spring Boot for automatic entity and request validation.

        ### **DTO Mapping**
        - `mapstruct` – Generates type-safe mappers between entities and DTOs.
        - `mapstruct-processor` – Annotation processor that works at compile time (scope `provided`).

        ### **Code Reduction**
        - `lombok` – Auto-generates getters, setters, constructors, builders, and other boilerplate code.

        ### **API Documentation**
        - `springdoc-openapi-starter-webmvc-ui` – Generates OpenAPI/Swagger UI automatically for REST endpoints.

        ### **Testing**
        - `spring-boot-starter-data-jpa-test` – JPA testing utilities including in-memory DB.
        - `spring-boot-starter-flyway-test` – Flyway migration testing support.
        - `spring-boot-starter-webmvc-test` – Testing utilities for Spring MVC (MockMvc, annotations).
        - `spring-boot-testcontainers` – Run integration tests in Docker containers.
        - `testcontainers-junit-jupiter` – JUnit 5 integration for Testcontainers.
        - `testcontainers-mysql` – MySQL container for integration testing.
        - `rest-assured` – Simplifies REST API testing with fluent syntax.

        ---

        ## 4. **Build Configuration**

        ### **Plugins**
        - **maven-compiler-plugin**
        - Configures annotation processing for Lombok and MapStruct.
        - Ensures MapStruct is processed **after** Lombok.
        - **spring-boot-maven-plugin**
        - Handles packaging the project as a Spring Boot executable JAR.
        - Excludes Lombok from the final artifact since it’s only compile-time.

        ---

        ## 5. **Summary**
        This POM provides all essential dependencies for:
        - REST API development
        - Database persistence with JPA
        - Database migrations with Flyway
        - Validation and DTO mapping
        - API documentation
        - Code reduction with Lombok
        - Testing including unit, integration, and containerized tests
        - Production monitoring

        It is fully equipped for **production-ready Spring Boot microservices** with clear separation of concerns and testability.

        ---

        ### **Tip**
        To update versions of major libraries (MapStruct, Rest-Assured, SpringDoc), modify the property values in `<properties>` section instead of changing each dependency individually.
