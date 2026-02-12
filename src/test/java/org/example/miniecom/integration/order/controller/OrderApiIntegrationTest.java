package org.example.miniecom.integration.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiIntegrationTest extends TestcontainersConfig {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void postOrders_createsOrderAndPersistsIt() throws Exception {
        String payload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": 11, "quantity": 2, "price": 9.99},
                    {"productId": 12, "quantity": 1, "price": 5.00}
                  ]
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(24.98));

        assertThat(orderRepository.count()).isEqualTo(1);
        Order order = orderRepository.findAll().getFirst();
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("24.98"));
    }

    @Test
    void postOrders_withEmptyItems_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "userId": 7,
                  "items": []
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.items").exists());
    }

    @Test
    void getOrdersAndById_returnsCreatedOrder() throws Exception {
        String payload = """
                {
                  "userId": 25,
                  "items": [
                    {"productId": 90, "quantity": 1, "price": 15.00}
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long orderId = createdBody.get("id").asLong();

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId));

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(25));
    }

    @Test
    void getOrder_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id=999999"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void postOrders_withInvalidItemFields_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "userId": 5,
                  "items": [
                    {"productId": null, "quantity": 0, "price": 0}
                  ]
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.validationErrors['items[0].quantity']").exists())
                .andExpect(jsonPath("$.validationErrors['items[0].price']").exists());
    }
}
