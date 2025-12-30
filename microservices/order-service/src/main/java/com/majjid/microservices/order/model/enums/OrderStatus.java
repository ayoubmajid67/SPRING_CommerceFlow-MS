package com.majjid.microservices.order.model.enums;

public enum OrderStatus {
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    UNDER_PROCESS("UNDER_PROCESS");


    final public static  String CHECK_CONSTRAINT = "CHECK (OrderStatus IN ('COMPLETED', 'CANCELLED', 'UNDER_PROCESS'))";
    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}