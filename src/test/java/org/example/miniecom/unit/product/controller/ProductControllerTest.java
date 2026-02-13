package org.example.miniecom.unit.product.controller;

import org.example.miniecom.common.GlobalExceptionHandler;
import org.example.miniecom.product.controller.ProductController;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.service.ProductNotFoundException;
import org.example.miniecom.product.service.ProductService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void postProducts_givenValidRequest_returnsCreatedProduct() throws Exception {
        Product saved = Product.builder()
                .id(100L)
                .name("Laptop")
                .price(new BigDecimal("1200.00"))
                .stock(8)
                .build();
        when(productService.createProduct(any())).thenReturn(saved);

        String payload = """
                {
                  "name": "Laptop",
                  "price": 1200.00,
                  "stock": 8
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(1200.00))
                .andExpect(jsonPath("$.stock").value(8));
    }

    @Test
    void postProducts_givenInvalidRequest_returnsBadRequest() throws Exception {
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
    void getProduct_givenExistingId_returnsProduct() throws Exception {
        when(productService.getProduct(5L)).thenReturn(Product.builder()
                .id(5L)
                .name("Mouse")
                .price(new BigDecimal("25.99"))
                .stock(12)
                .build());

        mockMvc.perform(get("/products/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(25.99))
                .andExpect(jsonPath("$.stock").value(12));
    }

    @Test
    void getProduct_givenUnknownId_returnsNotFound() throws Exception {
        when(productService.getProduct(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=999"));
    }

    @Test
    void getProducts_returnsAllProducts() throws Exception {
        when(productService.getProducts()).thenReturn(List.of(
                Product.builder().id(1L).name("A").price(new BigDecimal("10.00")).stock(1).build(),
                Product.builder().id(2L).name("B").price(new BigDecimal("20.00")).stock(2).build()
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("B"));
    }

    @Test
    void putProducts_givenValidRequest_returnsUpdatedProduct() throws Exception {
        Product updated = Product.builder()
                .id(12L)
                .name("Updated Monitor")
                .price(new BigDecimal("250.00"))
                .stock(9)
                .build();
        when(productService.updateProduct(any(), any())).thenReturn(updated);

        String payload = """
                {
                  "name": "Updated Monitor",
                  "price": 250.00,
                  "stock": 9
                }
                """;

        mockMvc.perform(put("/products/{id}", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.name").value("Updated Monitor"))
                .andExpect(jsonPath("$.price").value(250.00))
                .andExpect(jsonPath("$.stock").value(9));
    }

    @Test
    void putProducts_givenInvalidRequest_returnsBadRequest() throws Exception {
        String payload = """
                {
                  "name": "",
                  "price": 0,
                  "stock": -1
                }
                """;

        mockMvc.perform(put("/products/{id}", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stock").exists());
    }

    @Test
    void putProducts_givenUnknownId_returnsNotFound() throws Exception {
        when(productService.updateProduct(any(), any())).thenThrow(new ProductNotFoundException(555L));

        String payload = """
                {
                  "name": "Updated Monitor",
                  "price": 250.00,
                  "stock": 9
                }
                """;

        mockMvc.perform(put("/products/{id}", 555L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=555"));
    }

    @Test
    void deleteProducts_givenExistingId_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/products/{id}", 12L))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(12L);
    }

    @Test
    void deleteProducts_givenUnknownId_returnsNotFound() throws Exception {
        doThrow(new ProductNotFoundException(777L)).when(productService).deleteProduct(777L);

        mockMvc.perform(delete("/products/{id}", 777L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with id=777"));
    }
}
