package org.example.miniecom.order.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id=" + orderId);
    }
}
