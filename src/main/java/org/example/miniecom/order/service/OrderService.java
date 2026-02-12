package org.example.miniecom.order.service;

import lombok.RequiredArgsConstructor;
import org.example.miniecom.order.dto.request.CreateOrderItemRequest;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderItem;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Order order = Order.builder()
                .userId(request.userId())
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .quantity(itemRequest.quantity())
                    .price(itemRequest.price())
                    .build();
            order.addItem(item);

            total = total.add(itemRequest.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders() {
        return orderRepository.findAll();
    }
}
