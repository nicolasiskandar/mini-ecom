package org.example.miniecom.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderItemRequest(
        @NotNull(message = "productId is required")
        Long productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be positive")
        Integer quantity,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price
) {
}
