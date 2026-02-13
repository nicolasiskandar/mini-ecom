package org.example.miniecom.payment.service;

import org.example.miniecom.order.domain.Order;
import org.example.miniecom.payment.dto.request.PaymentRequest;

public interface PaymentGateway {

    PaymentGatewayResult charge(Order order, PaymentRequest paymentRequest);
}
