package org.example.miniecom.product.service;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("Product not found with id=" + productId);
    }
}
