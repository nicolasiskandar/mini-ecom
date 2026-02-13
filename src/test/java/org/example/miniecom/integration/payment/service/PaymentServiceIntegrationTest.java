package org.example.miniecom.integration.payment.service;

import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.payment.domain.PaymentMethod;
import org.example.miniecom.payment.dto.request.PaymentRequest;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.example.miniecom.payment.service.PaymentGateway;
import org.example.miniecom.payment.service.PaymentProcessingException;
import org.example.miniecom.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceIntegrationTest extends TestcontainersConfig {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void processPayment_whenGatewayThrows_rollsBackTransaction() {
        Order order = orderRepository.save(Order.builder()
                .userId(33L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("19.99"))
                .build());

        when(paymentGateway.charge(any(), any())).thenThrow(new RuntimeException("Gateway unavailable"));

        assertThatThrownBy(() -> paymentService.processPayment(
                order.getId(),
                new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_tok_ok")
        )).isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Payment processing failed for orderId=" + order.getId());

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findByOrderId(order.getId())).isEmpty();
    }
}
