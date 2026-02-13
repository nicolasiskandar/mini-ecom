package org.example.miniecom.payment.service;

import org.example.miniecom.order.domain.Order;
import org.example.miniecom.payment.domain.PaymentMethod;
import org.example.miniecom.payment.dto.request.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayResult charge(Order order, PaymentRequest paymentRequest) {
        if (paymentRequest.method() == PaymentMethod.CREDIT_CARD) {
            if (paymentRequest.paymentToken().startsWith("cc_fail")) {
                return new PaymentGatewayResult(false, null, "Credit card was declined");
            }
            return new PaymentGatewayResult(true, "cc_" + UUID.randomUUID(), null);
        }

        if (paymentRequest.paymentToken().startsWith("pp_fail")) {
            return new PaymentGatewayResult(false, null, "PayPal authorization failed");
        }
        return new PaymentGatewayResult(true, "pp_" + UUID.randomUUID(), null);
    }
}
