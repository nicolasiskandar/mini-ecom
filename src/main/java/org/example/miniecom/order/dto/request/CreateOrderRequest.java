package org.example.miniecom.order.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @Schema(description = "Customer ID placing the order", example = "101")
        @NotNull(message = "userId is required")
        Long userId,
        @ArraySchema(schema = @Schema(implementation = CreateOrderItemRequest.class), minItems = 1)
        @NotEmpty(message = "items must not be empty")
        List<@Valid CreateOrderItemRequest> items
) {
}
