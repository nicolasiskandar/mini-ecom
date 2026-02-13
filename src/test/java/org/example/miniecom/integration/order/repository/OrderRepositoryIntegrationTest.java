package org.example.miniecom.integration.order.repository;

import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderItem;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest extends TestcontainersConfig {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void save_persistsOrderAndItemsWithCascade() {
        Order order = Order.builder()
                .userId(99L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("29.98"))
                .build();
        order.addItem(OrderItem.builder().productId(1L).quantity(2).price(new BigDecimal("9.99")).build());
        order.addItem(OrderItem.builder().productId(2L).quantity(1).price(new BigDecimal("10.00")).build());

        Order saved = orderRepository.saveAndFlush(order);

        Optional<Order> loaded = orderRepository.findById(saved.getId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getItems()).hasSize(2);
        assertThat(loaded.get().getTotalAmount()).isEqualByComparingTo("29.98");
        assertThat(loaded.get().getItems()).allMatch(item -> item.getOrder().getId().equals(saved.getId()));
    }

    @Test
    void save_whenUpdated_refreshesUpdatedAt() throws InterruptedException {
        Order order = Order.builder()
                .userId(7L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("15.00"))
                .build();

        Order created = orderRepository.saveAndFlush(order);
        LocalDateTime createdAt = created.getCreatedAt();
        LocalDateTime firstUpdatedAt = created.getUpdatedAt();

        Thread.sleep(10);

        created.setStatus(OrderStatus.PAID);
        Order updated = orderRepository.saveAndFlush(created);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(firstUpdatedAt);
    }

    @Test
    void save_whenStatusIsNull_defaultsStatusToCreated() {
        Order order = Order.builder()
                .userId(42L)
                .status(null)
                .totalAmount(new BigDecimal("10.00"))
                .build();

        Order saved = orderRepository.saveAndFlush(order);

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void delete_removesOrderAndChildItems() {
        Order order = Order.builder()
                .userId(15L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("5.00"))
                .build();
        order.addItem(OrderItem.builder().productId(3L).quantity(1).price(new BigDecimal("5.00")).build());

        Order saved = orderRepository.saveAndFlush(order);
        Long orderId = saved.getId();

        orderRepository.deleteById(orderId);
        orderRepository.flush();

        Integer orderCount = jdbcTemplate.queryForObject(
                "select count(*) from orders where id = ?",
                Integer.class,
                orderId
        );
        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from order_items where order_id = ?",
                Integer.class,
                orderId
        );

        assertThat(orderCount).isZero();
        assertThat(itemCount).isZero();
    }

    @Test
    void findWithItemsById_loadsOrderWithItems() {
        Order order = Order.builder()
                .userId(51L)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("30.00"))
                .build();
        order.addItem(OrderItem.builder().productId(5L).quantity(3).price(new BigDecimal("10.00")).build());
        Order saved = orderRepository.saveAndFlush(order);

        Optional<Order> loaded = orderRepository.findWithItemsById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getItems()).hasSize(1);
        assertThat(loaded.get().getItems().getFirst().getProductId()).isEqualTo(5L);
    }
}
