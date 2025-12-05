package com.majjid.microservices.order.model;

import com.majjid.microservices.order.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "t_order")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * The unique identifier for the entire customer order or transaction.
     * An order can contain multiple different products (line items).
     * Example: "ORD-2023-98765"
     */
    private String orderNumber;

    /**
     * The Stock Keeping Unit (SKU) code, which is a unique identifier
     * for a specific product variation in the inventory.
     * Example: "TSHIRT-BLUE-L" for a Large, Blue T-Shirt.
     */
    private String skuCode;

    /**
     * The status of the order (e.g., PENDING, SHIPPED, CANCELLED).
     * Stored as a string in the database for readability.
     */
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private BigDecimal price;
    private Integer quantity;
}
