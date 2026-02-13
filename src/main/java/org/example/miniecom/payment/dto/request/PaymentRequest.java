package org.example.miniecom.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.miniecom.payment.domain.PaymentMethod;

public record PaymentRequest(
        @NotNull(message = "method is required")
        PaymentMethod method,
        @NotBlank(message = "paymentToken is required")
        String paymentToken
) {
}
