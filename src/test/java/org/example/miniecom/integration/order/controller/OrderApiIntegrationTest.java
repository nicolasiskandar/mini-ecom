package org.example.miniecom.integration.order.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.order.domain.Order;
import org.example.miniecom.order.repository.OrderRepository;
import org.example.miniecom.payment.repository.PaymentRepository;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.repository.ProductRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Long headphonesProductId;
    private Long standProductId;
    private Long cableProductId;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();

        Product headphones = productRepository.save(Product.builder()
                .name("Headphones")
                .price(new BigDecimal("9.99"))
                .stock(10)
                .build());
        Product stand = productRepository.save(Product.builder()
                .name("Stand")
                .price(new BigDecimal("5.00"))
                .stock(10)
                .build());
        Product cable = productRepository.save(Product.builder()
                .name("Cable")
                .price(new BigDecimal("15.00"))
                .stock(10)
                .build());

        headphonesProductId = headphones.getId();
        standProductId = stand.getId();
        cableProductId = cable.getId();
    }

    @Test
    void postOrders_createsOrderAndPersistsIt() throws Exception {
        String payload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 2},
                    {"productId": %d, "amount": 1}
                  ]
                }
                """.formatted(headphonesProductId, standProductId);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.status").value("PENDING"))
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
                    {"productId": %d, "amount": 1}
                  ]
                }
                """.formatted(cableProductId);

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
                .andExpect(jsonPath("$.userId").value(25))
                .andExpect(jsonPath("$.totalAmount").value(15.0))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(cableProductId))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].price").value(15.0));
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
                    {"productId": null, "amount": 0}
                  ]
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.validationErrors['items[0].amount']").exists());
    }

    @Test
    void postOrders_withInsufficientStock_returnsBadRequestAndDoesNotPersistOrder() throws Exception {
        Product limited = productRepository.save(Product.builder()
                .name("Limited Item")
                .price(new BigDecimal("7.50"))
                .stock(1)
                .build());

        String payload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 2}
                  ]
                }
                """.formatted(limited.getId());

                mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for productId=" + limited.getId()));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void postOrders_withUnknownProductId_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": 999999, "amount": 1}
                  ]
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid productId=999999"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void putOrders_updatesOrderAndReconcilesStock() throws Exception {
        Product originalProduct = productRepository.save(Product.builder()
                .name("Original Product")
                .price(new BigDecimal("4.00"))
                .stock(10)
                .build());
        Product newProduct = productRepository.save(Product.builder()
                .name("New Product")
                .price(new BigDecimal("3.00"))
                .stock(10)
                .build());

        String createPayload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 2}
                  ]
                }
                """.formatted(originalProduct.getId());
        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String updatePayload = """
                {
                  "userId": 9,
                  "items": [
                    {"productId": %d, "amount": 1},
                    {"productId": %d, "amount": 3}
                  ]
                }
                """.formatted(originalProduct.getId(), newProduct.getId());

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(9))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalAmount").value(13.0));

        Product reloadedOriginal = productRepository.findById(originalProduct.getId()).orElseThrow();
        Product reloadedNew = productRepository.findById(newProduct.getId()).orElseThrow();
        assertThat(reloadedOriginal.getStock()).isEqualTo(9);
        assertThat(reloadedNew.getStock()).isEqualTo(7);
    }

    @Test
    void putOrders_withUnknownId_returnsNotFound() throws Exception {
        String payload = """
                {
                  "userId": 9,
                  "items": [
                    {"productId": %d, "amount": 1}
                  ]
                }
                """.formatted(headphonesProductId);

        mockMvc.perform(put("/orders/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id=999999"));
    }

    @Test
    void putOrders_withInsufficientStock_returnsBadRequest() throws Exception {
        Product limited = productRepository.save(Product.builder()
                .name("Limited Update Product")
                .price(new BigDecimal("20.00"))
                .stock(2)
                .build());

        String createPayload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 1}
                  ]
                }
                """.formatted(limited.getId());
        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String updatePayload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 3}
                  ]
                }
                """.formatted(limited.getId());

        mockMvc.perform(put("/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock for productId=" + limited.getId()));

        Product reloadedLimited = productRepository.findById(limited.getId()).orElseThrow();
        Order reloadedOrder = orderRepository.findWithItemsById(orderId).orElseThrow();
        assertThat(reloadedLimited.getStock()).isEqualTo(1);
        assertThat(reloadedOrder.getUserId()).isEqualTo(7L);
        assertThat(reloadedOrder.getTotalAmount()).isEqualByComparingTo("20.00");
        assertThat(reloadedOrder.getItems()).hasSize(1);
        assertThat(reloadedOrder.getItems().getFirst().getProductId()).isEqualTo(limited.getId());
        assertThat(reloadedOrder.getItems().getFirst().getQuantity()).isEqualTo(1);
    }

    @Test
    void deleteOrders_removesOrderAndRestoresStock() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("To Delete Product")
                .price(new BigDecimal("7.00"))
                .stock(10)
                .build());

        String createPayload = """
                {
                  "userId": 7,
                  "items": [
                    {"productId": %d, "amount": 3}
                  ]
                }
                """.formatted(product.getId());
        MvcResult createResult = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/orders/{id}", orderId))
                .andExpect(status().isNotFound());

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStock()).isEqualTo(10);
    }

    @Test
    void deleteOrders_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/orders/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found with id=999999"));
    }
}
