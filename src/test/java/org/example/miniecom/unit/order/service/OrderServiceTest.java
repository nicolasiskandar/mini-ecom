package org.example.miniecom.unit.order.service;

import org.example.miniecom.order.dto.request.CreateOrderItemRequest;
import org.example.miniecom.order.dto.request.CreateOrderRequest;
import org.example.miniecom.order.dto.request.UpdateOrderRequest;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderItem;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.order.service.InsufficientStockException;
import org.example.miniecom.order.service.InvalidOrderItemsException;
import org.example.miniecom.order.service.InvalidOrderProductException;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.repository.ProductRepository;
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

    @Mock
    private ProductRepository productRepository;

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
        when(productRepository.findById(1001L)).thenReturn(Optional.of(Product.builder()
                .id(1001L)
                .name("Product-1001")
                .stock(100)
                .price(new BigDecimal("10.00"))
                .build()));
        when(productRepository.findById(1002L)).thenReturn(Optional.of(Product.builder()
                .id(1002L)
                .name("Product-1002")
                .stock(100)
                .price(new BigDecimal("5.50"))
                .build()));

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
                .isInstanceOf(InvalidOrderItemsException.class)
                .hasMessageContaining("at least one item");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_givenNullItems_throwsValidationError() {
        CreateOrderRequest request = new CreateOrderRequest(42L, null);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOrderItemsException.class)
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
        when(productRepository.findById(10L)).thenReturn(Optional.of(Product.builder()
                .id(10L)
                .name("Product-10")
                .stock(100)
                .price(new BigDecimal("0.10"))
                .build()));
        when(productRepository.findById(11L)).thenReturn(Optional.of(Product.builder()
                .id(11L)
                .name("Product-11")
                .stock(100)
                .price(new BigDecimal("0.25"))
                .build()));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(request);

        assertThat(result.getTotalAmount()).isEqualByComparingTo("1.30");
    }

    @Test
    void createOrder_givenSufficientStock_reducesStockPerProduct() {
        Product product = Product.builder()
                .id(501L)
                .name("Keyboard")
                .stock(5)
                .price(new BigDecimal("20.00"))
                .build();
        when(productRepository.findById(501L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(
                12L,
                List.of(new CreateOrderItemRequest(501L, 3, new BigDecimal("20.00")))
        );

        orderService.createOrder(request);

        assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    void createOrder_givenInsufficientStock_throwsAndDoesNotPersistOrder() {
        Product product = Product.builder()
                .id(777L)
                .name("Mouse")
                .stock(1)
                .price(new BigDecimal("12.00"))
                .build();
        when(productRepository.findById(777L)).thenReturn(Optional.of(product));

        CreateOrderRequest request = new CreateOrderRequest(
                12L,
                List.of(new CreateOrderItemRequest(777L, 2, new BigDecimal("12.00")))
        );

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("777");

        verify(orderRepository, never()).save(any(Order.class));
        assertThat(product.getStock()).isEqualTo(1);
    }

    @Test
    void createOrder_givenUnknownProductId_throwsAndDoesNotPersistOrder() {
        when(productRepository.findById(12345L)).thenReturn(Optional.empty());

        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                List.of(new CreateOrderItemRequest(12345L, 1, new BigDecimal("10.00")))
        );

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOrderProductException.class)
                .hasMessageContaining("Invalid productId=12345");

        verify(orderRepository, never()).save(any(Order.class));
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

    @Test
    void updateOrder_givenValidRequest_replacesItemsAndAdjustsStock() {
        Product product1 = Product.builder()
                .id(1001L)
                .name("Product-1001")
                .stock(4)
                .price(new BigDecimal("10.00"))
                .build();
        Product product2 = Product.builder()
                .id(1002L)
                .name("Product-1002")
                .stock(5)
                .price(new BigDecimal("5.00"))
                .build();
        when(productRepository.findById(1001L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(1002L)).thenReturn(Optional.of(product2));

        Order existing = Order.builder()
                .id(30L)
                .userId(9L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("20.00"))
                .build();
        existing.addItem(OrderItem.builder()
                .productId(1001L)
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build());

        when(orderRepository.findWithItemsById(30L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderRequest request = new UpdateOrderRequest(
                11L,
                List.of(
                        new CreateOrderItemRequest(1001L, 1, new BigDecimal("10.00")),
                        new CreateOrderItemRequest(1002L, 3, new BigDecimal("5.00"))
                )
        );

        Order updated = orderService.updateOrder(30L, request);

        assertThat(updated.getUserId()).isEqualTo(11L);
        assertThat(updated.getItems()).hasSize(2);
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("25.00");
        assertThat(product1.getStock()).isEqualTo(5);
        assertThat(product2.getStock()).isEqualTo(2);

        verify(orderRepository, times(1)).findWithItemsById(30L);
        verify(orderRepository, times(1)).save(existing);
    }

    @Test
    void updateOrder_givenUnknownId_throwsNotFound() {
        when(orderRepository.findWithItemsById(404L)).thenReturn(Optional.empty());

        UpdateOrderRequest request = new UpdateOrderRequest(
                11L,
                List.of(new CreateOrderItemRequest(1001L, 1, new BigDecimal("10.00")))
        );

        assertThatThrownBy(() -> orderService.updateOrder(404L, request))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("404");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_givenInsufficientStock_throwsAndDoesNotPersist() {
        Product product = Product.builder()
                .id(1001L)
                .name("Product-1001")
                .stock(1)
                .price(new BigDecimal("10.00"))
                .build();
        when(productRepository.findById(1001L)).thenReturn(Optional.of(product));

        Order existing = Order.builder()
                .id(30L)
                .userId(9L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("10.00"))
                .build();
        existing.addItem(OrderItem.builder()
                .productId(1001L)
                .quantity(1)
                .price(new BigDecimal("10.00"))
                .build());
        when(orderRepository.findWithItemsById(30L)).thenReturn(Optional.of(existing));

        UpdateOrderRequest request = new UpdateOrderRequest(
                11L,
                List.of(new CreateOrderItemRequest(1001L, 5, new BigDecimal("10.00")))
        );

        assertThatThrownBy(() -> orderService.updateOrder(30L, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock")
                .hasMessageContaining("1001");

        assertThat(product.getStock()).isEqualTo(1);
        assertThat(existing.getItems()).hasSize(1);
        assertThat(existing.getItems().getFirst().getQuantity()).isEqualTo(1);
        verify(productRepository, never()).saveAll(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_whenRemovingExistingItem_restoresRemovedProductStock() {
        Product removedProduct = Product.builder()
                .id(1001L)
                .name("Product-1001")
                .stock(4)
                .price(new BigDecimal("10.00"))
                .build();
        Product keptProduct = Product.builder()
                .id(1002L)
                .name("Product-1002")
                .stock(8)
                .price(new BigDecimal("5.00"))
                .build();
        when(productRepository.findById(1001L)).thenReturn(Optional.of(removedProduct));
        when(productRepository.findById(1002L)).thenReturn(Optional.of(keptProduct));

        Order existing = Order.builder()
                .id(31L)
                .userId(9L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("25.00"))
                .build();
        existing.addItem(OrderItem.builder()
                .productId(1001L)
                .quantity(2)
                .price(new BigDecimal("10.00"))
                .build());
        existing.addItem(OrderItem.builder()
                .productId(1002L)
                .quantity(1)
                .price(new BigDecimal("5.00"))
                .build());

        when(orderRepository.findWithItemsById(31L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateOrderRequest request = new UpdateOrderRequest(
                11L,
                List.of(new CreateOrderItemRequest(1002L, 1, new BigDecimal("5.00")))
        );

        Order updated = orderService.updateOrder(31L, request);

        assertThat(updated.getItems()).hasSize(1);
        assertThat(updated.getItems().getFirst().getProductId()).isEqualTo(1002L);
        assertThat(removedProduct.getStock()).isEqualTo(6);
        assertThat(keptProduct.getStock()).isEqualTo(8);
    }

    @Test
    void deleteOrder_givenExistingId_restoresStockAndDeletesOrder() {
        Product product = Product.builder()
                .id(501L)
                .name("Keyboard")
                .stock(2)
                .price(new BigDecimal("20.00"))
                .build();
        when(productRepository.findById(501L)).thenReturn(Optional.of(product));

        Order existing = Order.builder()
                .id(77L)
                .userId(12L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("60.00"))
                .build();
        existing.addItem(OrderItem.builder()
                .productId(501L)
                .quantity(3)
                .price(new BigDecimal("20.00"))
                .build());
        when(orderRepository.findWithItemsById(77L)).thenReturn(Optional.of(existing));

        orderService.deleteOrder(77L);

        assertThat(product.getStock()).isEqualTo(5);
        verify(orderRepository, times(1)).delete(existing);
    }

    @Test
    void deleteOrder_givenUnknownId_throwsNotFound() {
        when(orderRepository.findWithItemsById(900L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(900L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("900");

        verify(orderRepository, never()).delete(any(Order.class));
    }
}
