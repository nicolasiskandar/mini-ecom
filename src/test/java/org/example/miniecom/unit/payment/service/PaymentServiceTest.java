package org.example.miniecom.unit.payment.service;

import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.order.service.OrderNotFoundException;
import org.example.miniecom.order.service.OrderValidationException;
import org.example.miniecom.payment.domain.Payment;
import org.example.miniecom.payment.domain.PaymentMethod;
import org.example.miniecom.payment.domain.PaymentStatus;
import org.example.miniecom.payment.dto.request.PaymentRequest;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.example.miniecom.payment.service.PaymentGateway;
import org.example.miniecom.payment.service.PaymentGatewayResult;
import org.example.miniecom.payment.service.PaymentProcessingException;
import org.example.miniecom.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void processPayment_whenGatewaySucceeds_updatesOrderToPaid() {
        Order order = Order.builder()
                .id(10L)
                .userId(42L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("49.99"))
                .build();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_tok_ok");

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(order, request))
                .thenReturn(new PaymentGatewayResult(true, "cc_tx_123", null));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Payment result = paymentService.processPayment(10L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.getTransactionId()).isEqualTo("cc_tx_123");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("49.99");
    }

    @Test
    void processPayment_whenGatewayDeclines_keepsOrderPending() {
        Order order = Order.builder()
                .id(11L)
                .userId(7L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("20.00"))
                .build();
        PaymentRequest request = new PaymentRequest(PaymentMethod.PAYPAL, "pp_fail_token");

        when(orderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(order, request))
                .thenReturn(new PaymentGatewayResult(false, null, "PayPal authorization failed"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment result = paymentService.processPayment(11L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("PayPal authorization failed");

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void processPayment_whenOrderNotFound_throws() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(
                999L,
                new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_tok_ok")
        )).isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("999");

        verify(paymentGateway, never()).charge(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_whenOrderIsNotPending_throwsValidationError() {
        Order order = Order.builder()
                .id(12L)
                .userId(3L)
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("15.00"))
                .build();
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(
                12L,
                new PaymentRequest(PaymentMethod.PAYPAL, "pp_tok_ok")
        )).isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Only pending orders can be paid");

        verify(paymentGateway, never()).charge(any(), any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void processPayment_whenGatewayThrows_wrapsInPaymentProcessingException() {
        Order order = Order.builder()
                .id(13L)
                .userId(4L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("10.00"))
                .build();
        PaymentRequest request = new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_tok_ok");
        when(orderRepository.findById(13L)).thenReturn(Optional.of(order));
        when(paymentGateway.charge(order, request)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> paymentService.processPayment(13L, request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Payment processing failed for orderId=13");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentRepository, never()).save(any());
    }
}
