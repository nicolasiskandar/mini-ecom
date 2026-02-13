package org.example.miniecom.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price,
        @NotNull(message = "stock is required")
        @PositiveOrZero(message = "stock must be zero or positive")
        Integer stock
) {
}
