package org.example.miniecom.order.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(
        @Schema(description = "Product ID", example = "1")
        @NotNull(message = "productId is required")
        Long productId,
        @Schema(description = "Quantity of the product", example = "2")
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        Integer amount
) {
}
