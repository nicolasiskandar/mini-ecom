package org.example.miniecom.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @NotNull(message = "productId is required")
        Long productId,
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        Integer amount
) {
}
