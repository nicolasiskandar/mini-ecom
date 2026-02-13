package org.example.miniecom.unit.payment.service;

import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.payment.domain.PaymentMethod;
import org.example.miniecom.payment.dto.request.PaymentRequest;
import org.example.miniecom.payment.service.DefaultPaymentGateway;
import org.example.miniecom.payment.service.PaymentGatewayResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPaymentGatewayTest {

    private final DefaultPaymentGateway gateway = new DefaultPaymentGateway();

    @Test
    void charge_creditCardSuccess_returnsSucceededResultWithCcTransactionPrefix() {
        PaymentGatewayResult result = gateway.charge(order(), new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_tok_ok"));

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("cc_");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void charge_creditCardDecline_returnsFailure() {
        PaymentGatewayResult result = gateway.charge(order(), new PaymentRequest(PaymentMethod.CREDIT_CARD, "cc_fail_declined"));

        assertThat(result.success()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.failureReason()).isEqualTo("Credit card was declined");
    }

    @Test
    void charge_paypalSuccess_returnsSucceededResultWithPaypalTransactionPrefix() {
        PaymentGatewayResult result = gateway.charge(order(), new PaymentRequest(PaymentMethod.PAYPAL, "pp_tok_ok"));

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("pp_");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void charge_paypalFailure_returnsFailure() {
        PaymentGatewayResult result = gateway.charge(order(), new PaymentRequest(PaymentMethod.PAYPAL, "pp_fail_auth"));

        assertThat(result.success()).isFalse();
        assertThat(result.transactionId()).isNull();
        assertThat(result.failureReason()).isEqualTo("PayPal authorization failed");
    }

    private Order order() {
        return Order.builder()
                .id(1L)
                .userId(7L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("12.34"))
                .build();
    }
}
