package org.example.miniecom.integration.product.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.miniecom.integration.support.TestcontainersConfig;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductApiIntegrationTest extends TestcontainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void postProducts_createsProductAndPersistsIt() throws Exception {
        String payload = """
                {
                  "name": "Monitor",
                  "price": 199.90,
                  "stock": 14
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Monitor"))
                .andExpect(jsonPath("$.price").value(199.90))
                .andExpect(jsonPath("$.stock").value(14))
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long productId = createdBody.get("id").asLong();
        assertThat(productRepository.count()).isEqualTo(1);
        Product saved = productRepository.findById(productId).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Monitor");
        assertThat(saved.getPrice()).isEqualByComparingTo("199.90");
        assertThat(saved.getStock()).isEqualTo(14);
    }

    @Test
    void postProducts_withInvalidBody_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "name": "",
                  "price": 0,
                  "stock": -1
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stock").exists());
    }

    @Test
    void getProductsAndById_returnsPersistedProducts() throws Exception {
        Product keyboard = productRepository.save(Product.builder()
                .name("Keyboard")
                .price(new BigDecimal("49.99"))
                .stock(20)
                .build());
        Product webcam = productRepository.save(Product.builder()
                .name("Webcam")
                .price(new BigDecimal("89.90"))
                .stock(7)
                .build());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        keyboard.getId().intValue(),
                        webcam.getId().intValue()
                )));

        mockMvc.perform(get("/products/{id}", keyboard.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(keyboard.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(49.99))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void getProduct_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/products/{id}", 404L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=404"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void postThenGetById_returnsCreatedProduct() throws Exception {
        String payload = """
                {
                  "name": "Dock",
                  "price": 79.00,
                  "stock": 5
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long productId = createdBody.get("id").asLong();

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) productId))
                .andExpect(jsonPath("$.name").value("Dock"))
                .andExpect(jsonPath("$.price").value(79.00))
                .andExpect(jsonPath("$.stock").value(5));
    }

    @Test
    void putProducts_updatesProductAndPersistsIt() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Old Name")
                .price(new BigDecimal("10.00"))
                .stock(4)
                .build());

        String payload = """
                {
                  "name": "New Name",
                  "price": 15.75,
                  "stock": 8
                }
                """;

        mockMvc.perform(put("/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().intValue()))
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.price").value(15.75))
                .andExpect(jsonPath("$.stock").value(8));

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("New Name");
        assertThat(reloaded.getPrice()).isEqualByComparingTo("15.75");
        assertThat(reloaded.getStock()).isEqualTo(8);
    }

    @Test
    void putProducts_withInvalidBody_returnsBadRequest() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Any")
                .price(new BigDecimal("10.00"))
                .stock(4)
                .build());

        String payload = """
                {
                  "name": "",
                  "price": 0,
                  "stock": -1
                }
                """;

        mockMvc.perform(put("/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stock").exists());
    }

    @Test
    void putProducts_withUnknownId_returnsNotFound() throws Exception {
        String payload = """
                {
                  "name": "New Name",
                  "price": 15.75,
                  "stock": 8
                }
                """;

        mockMvc.perform(put("/products/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=999999"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deleteProducts_removesProduct() throws Exception {
        Product saved = productRepository.save(Product.builder()
                .name("Delete Me")
                .price(new BigDecimal("80.00"))
                .stock(3)
                .build());

        mockMvc.perform(delete("/products/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteProducts_withUnknownId_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/products/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=999999"));
    }
}
