package org.example.miniecom.order.service;

public class InsufficientStockException extends OrderValidationException {

    public InsufficientStockException(Long productId) {
        super("Insufficient stock for productId=" + productId);
    }
}
