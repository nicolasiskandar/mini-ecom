package org.example.miniecom.payment.service;

public record PaymentGatewayResult(
        boolean success,
        String transactionId,
        String failureReason
) {
}
