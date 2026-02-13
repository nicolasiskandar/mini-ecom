package org.example.miniecom.order.service;

public class InvalidOrderProductException extends OrderValidationException {

    public InvalidOrderProductException(Long productId) {
        super("Invalid productId=" + productId);
    }
}
