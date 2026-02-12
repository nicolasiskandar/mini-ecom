package org.example.miniecom.unit.order.service;

import org.example.miniecom.order.dto.request.CreateOrderItemRequest;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.order.service.OrderNotFoundException;
import org.example.miniecom.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_givenValidRequest_persistsOrderAndCalculatesTotal() {
        CreateOrderRequest request = new CreateOrderRequest(
                42L,
                List.of(
                        new CreateOrderItemRequest(1001L, 2, new BigDecimal("10.00")),
                        new CreateOrderItemRequest(1002L, 1, new BigDecimal("5.50"))
                )
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Order result = orderService.createOrder(request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());

        Order persisted = captor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(42L);
        assertThat(persisted.getItems()).hasSize(2);
        assertThat(persisted.getTotalAmount()).isEqualByComparingTo("25.50");
        assertThat(persisted.getItems()).allMatch(item -> item.getOrder() == persisted);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("25.50");
    }

    @Test
    void createOrder_givenEmptyItems_throwsValidationError() {
        CreateOrderRequest request = new CreateOrderRequest(42L, List.of());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_givenNullItems_throwsValidationError() {
        CreateOrderRequest request = new CreateOrderRequest(42L, null);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_givenDecimalPrices_calculatesPreciseTotal() {
        CreateOrderRequest request = new CreateOrderRequest(
                7L,
                List.of(
                        new CreateOrderItemRequest(10L, 3, new BigDecimal("0.10")),
                        new CreateOrderItemRequest(11L, 4, new BigDecimal("0.25"))
                )
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(request);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("1.30");
    }

    @Test
    void getOrder_givenExistingId_returnsOrder() {
        Order order = Order.builder()
                .id(10L)
                .userId(42L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("99.99"))
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        Order result = orderService.getOrder(10L);

        verify(orderRepository, times(1)).findById(10L);
        assertThat(result).isSameAs(order);
    }

    @Test
    void getOrder_givenMissingId_throwsNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");

        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void getOrders_returnsAllOrdersFromRepository() {
        List<Order> orders = List.of(
                Order.builder().id(1L).userId(42L).status(OrderStatus.CREATED).totalAmount(new BigDecimal("10.00")).build(),
                Order.builder().id(2L).userId(43L).status(OrderStatus.CREATED).totalAmount(new BigDecimal("20.00")).build()
        );
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.getOrders();

        verify(orderRepository, times(1)).findAll();
        assertThat(result).containsExactlyElementsOf(orders);
    }
}
