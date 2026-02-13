package org.example.miniecom.integration.order.controller;

import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderPaymentApiIntegrationTest extends TestcontainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void postOrdersPay_withCreditCard_successfullyPaysOrder() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .userId(12L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("39.50"))
                .build());

        String payload = """
                {
                  "method": "CREDIT_CARD",
                  "paymentToken": "cc_tok_ok"
                }
                """;

        mockMvc.perform(post("/orders/{id}/pay", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount").value(39.5))
                .andExpect(jsonPath("$.transactionId").exists());

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paymentRepository.findByOrderId(order.getId())).hasSize(1);
    }

    @Test
    void postOrdersPay_whenPayPalDeclined_keepsOrderPending() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .userId(13L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("25.00"))
                .build());

        String payload = """
                {
                  "method": "PAYPAL",
                  "paymentToken": "pp_fail_anything"
                }
                """;

        mockMvc.perform(post("/orders/{id}/pay", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("PayPal authorization failed"));

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(paymentRepository.findByOrderId(order.getId())).hasSize(1);
    }
}
