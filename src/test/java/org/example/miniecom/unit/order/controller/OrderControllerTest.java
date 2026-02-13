package org.example.miniecom.unit.order.controller;

import org.example.miniecom.common.GlobalExceptionHandler;
import org.example.miniecom.order.controller.OrderController;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.domain.OrderItem;
import org.example.miniecom.order.domain.OrderStatus;
import org.example.miniecom.order.service.InvalidOrderProductException;
import org.example.miniecom.order.service.OrderNotFoundException;
import org.example.miniecom.order.service.OrderService;
import org.example.miniecom.payment.domain.Payment;
import org.example.miniecom.payment.domain.PaymentMethod;
import org.example.miniecom.payment.domain.PaymentStatus;
import org.example.miniecom.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void postOrders_givenUnknownProductId_returnsBadRequest() throws Exception {
        when(orderService.createOrder(any()))
                .thenThrow(new InvalidOrderProductException(999L));

        String payload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": 999, "amount": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid productId=999"));
    }

    @Test
    void putOrders_givenValidRequest_returnsUpdatedOrder() throws Exception {
        Order updated = orderWithSingleItem(30L, 11L, 1001L, 2, "9.99", "19.98");
        when(orderService.updateOrder(any(), any())).thenReturn(updated);

        String payload = """
                {
                  "userId": 11,
                  "items": [
                    {"productId": 1001, "amount": 2}
                  ]
                }
                """;

        mockMvc.perform(put("/orders/{id}", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalAmount").value(19.98));
    }

    @Test
    void putOrders_givenInvalidRequest_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "userId": null,
                  "items": []
                }
                """;

        mockMvc.perform(put("/orders/{id}", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    void putOrders_givenUnknownId_returnsNotFound() throws Exception {
        when(orderService.updateOrder(any(), any())).thenThrow(new OrderNotFoundException(404L));

        String payload = """
                {
                  "userId": 11,
                  "items": [
                    {"productId": 1001, "amount": 2}
                  ]
                }
                """;

        mockMvc.perform(put("/orders/{id}", 404L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id=404"));
    }

    @Test
    void deleteOrders_givenExistingId_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/orders/{id}", 77L))
                .andExpect(status().isNoContent());

        verify(orderService).deleteOrder(77L);
    }

    @Test
    void deleteOrders_givenUnknownId_returnsNotFound() throws Exception {
        doThrow(new OrderNotFoundException(900L)).when(orderService).deleteOrder(900L);

        mockMvc.perform(delete("/orders/{id}", 900L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id=900"));
    }

    @Test
    void postOrdersPay_givenValidRequest_returnsPayment() throws Exception {
        Order order = orderWithSingleItem(88L, 5L, 123L, 1, "9.99", "9.99");
        Payment payment = Payment.builder()
                .id(501L)
                .order(order)
                .method(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.SUCCEEDED)
                .amount(new BigDecimal("9.99"))
                .transactionId("cc_tx_1")
                .build();
        when(paymentService.processPayment(any(), any())).thenReturn(payment);

        String payload = """
                {
                  "method": "CREDIT_CARD",
                  "paymentToken": "cc_tok_ok"
                }
                """;

        mockMvc.perform(post("/orders/{id}/pay", 88L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(501))
                .andExpect(jsonPath("$.orderId").value(88))
                .andExpect(jsonPath("$.method").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.amount").value(9.99))
                .andExpect(jsonPath("$.transactionId").value("cc_tx_1"));
    }

    private Order orderWithSingleItem(
            Long orderId,
            Long userId,
            Long productId,
            int quantity,
            String price,
            String total
    ) {
        Order order = Order.builder()
                .id(orderId)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal(total))
                .build();
        order.setItems(List.of(OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .price(new BigDecimal(price))
                .order(order)
                .build()));
        return order;
    }
}
