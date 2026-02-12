package org.example.miniecom.order.dto.response;

import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
