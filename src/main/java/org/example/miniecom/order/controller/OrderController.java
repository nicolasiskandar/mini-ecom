package org.example.miniecom.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.dto.request.UpdateOrderRequest;
import org.example.miniecom.order.dto.response.OrderResponse;
import org.example.miniecom.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = OrderResponse.from(orderService.createOrder(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        return orderService.getOrders().stream().map(OrderResponse::from).toList();
    }

    @PutMapping("/{id}")
    public OrderResponse updateOrder(@PathVariable Long id, @Valid @RequestBody UpdateOrderRequest request) {
        return OrderResponse.from(orderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
