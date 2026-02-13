package org.example.miniecom.order.service;

public class InvalidOrderItemsException extends OrderValidationException {

    public InvalidOrderItemsException() {
        super("Order must contain at least one item");
    }
}
