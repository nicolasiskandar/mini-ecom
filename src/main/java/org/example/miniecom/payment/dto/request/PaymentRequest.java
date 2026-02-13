package org.example.miniecom.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.miniecom.payment.domain.PaymentMethod;

public record PaymentRequest(
        @Schema(description = "Payment method", example = "CARD")
        @NotNull(message = "method is required")
        PaymentMethod method,
        @Schema(description = "Tokenized payment reference from gateway", example = "tok_visa_123456")
        @NotBlank(message = "paymentToken is required")
        String paymentToken
) {
}
