package org.example.miniecom.order.service;

public class OrderValidationException extends RuntimeException {

    public OrderValidationException(String message) {
        super(message);
    }
}
