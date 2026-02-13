package org.example.miniecom.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
        @Schema(description = "Product display name", example = "Mechanical Keyboard")
        @NotBlank(message = "name is required")
        String name,
        @Schema(description = "Unit price", example = "129.99")
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be positive")
        BigDecimal price,
        @Schema(description = "Available stock count", example = "35")
        @NotNull(message = "stock is required")
        @PositiveOrZero(message = "stock must be zero or positive")
        Integer stock
) {
}
