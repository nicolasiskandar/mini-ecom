package org.example.miniecom.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.order.service.OrderNotFoundException;
import org.example.miniecom.order.service.OrderValidationException;
import org.example.miniecom.payment.domain.Payment;
import org.example.miniecom.payment.domain.PaymentStatus;
import org.example.miniecom.payment.dto.request.PaymentRequest;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public Payment processPayment(Long orderId, PaymentRequest paymentRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderValidationException("Only pending orders can be paid");
        }

        PaymentGatewayResult gatewayResult;
        try {
            gatewayResult = paymentGateway.charge(order, paymentRequest);
        } catch (RuntimeException ex) {
            throw new PaymentProcessingException("Payment processing failed for orderId=" + orderId, ex);
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(paymentRequest.method())
                .amount(order.getTotalAmount())
                .status(gatewayResult.success() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED)
                .transactionId(gatewayResult.transactionId())
                .failureReason(gatewayResult.failureReason())
                .build();

        if (gatewayResult.success()) {
            order.setStatus(OrderStatus.PAID);
        }

        return paymentRepository.save(payment);
    }
}
