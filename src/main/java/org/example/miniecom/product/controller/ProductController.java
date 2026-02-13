package org.example.miniecom.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.miniecom.product.dto.request.CreateProductRequest;
import org.example.miniecom.product.dto.request.UpdateProductRequest;
import org.example.miniecom.product.dto.response.ProductResponse;
import org.example.miniecom.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse response = ProductResponse.from(productService.createProduct(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable Long id) {
        return ProductResponse.from(productService.getProduct(id));
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
        return productService.getProducts().stream().map(ProductResponse::from).toList();
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return ProductResponse.from(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
