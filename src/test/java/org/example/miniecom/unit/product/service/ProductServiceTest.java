package org.example.miniecom.unit.product.service;

import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.dto.request.CreateProductRequest;
import org.example.miniecom.product.dto.request.UpdateProductRequest;
import org.example.miniecom.product.repository.ProductRepository;
import org.example.miniecom.product.service.ProductNotFoundException;
import org.example.miniecom.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_givenValidRequest_persistsAndReturnsProduct() {
        CreateProductRequest request = new CreateProductRequest(
                "Monitor",
                new BigDecimal("199.90"),
                10
        );
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Product result = productService.createProduct(request);

        verify(productRepository, times(1)).save(any(Product.class));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Monitor");
        assertThat(result.getPrice()).isEqualByComparingTo("199.90");
        assertThat(result.getStock()).isEqualTo(10);
    }

    @Test
    void getProduct_givenExistingId_returnsProduct() {
        Product product = Product.builder()
                .id(42L)
                .name("Keyboard")
                .price(new BigDecimal("79.99"))
                .stock(7)
                .build();
        when(productRepository.findById(42L)).thenReturn(Optional.of(product));

        Product result = productService.getProduct(42L);

        verify(productRepository, times(1)).findById(42L);
        assertThat(result).isSameAs(product);
    }

    @Test
    void getProduct_givenMissingId_throwsNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");

        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void getProducts_returnsAllProductsFromRepository() {
        List<Product> products = List.of(
                Product.builder().id(1L).name("A").price(new BigDecimal("10.00")).stock(1).build(),
                Product.builder().id(2L).name("B").price(new BigDecimal("20.00")).stock(2).build()
        );
        when(productRepository.findAll()).thenReturn(products);

        List<Product> result = productService.getProducts();

        verify(productRepository, times(1)).findAll();
        assertThat(result).containsExactlyElementsOf(products);
    }

    @Test
    void updateProduct_givenExistingId_updatesAndReturnsProduct() {
        Product existing = Product.builder()
                .id(50L)
                .name("Old Name")
                .price(new BigDecimal("10.00"))
                .stock(1)
                .build();
        UpdateProductRequest request = new UpdateProductRequest(
                "New Name",
                new BigDecimal("15.50"),
                8
        );

        when(productRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        Product result = productService.updateProduct(50L, request);

        verify(productRepository, times(1)).findById(50L);
        verify(productRepository, times(1)).save(existing);
        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPrice()).isEqualByComparingTo("15.50");
        assertThat(result.getStock()).isEqualTo(8);
    }

    @Test
    void updateProduct_givenMissingId_throwsNotFound() {
        UpdateProductRequest request = new UpdateProductRequest(
                "New Name",
                new BigDecimal("15.50"),
                8
        );
        when(productRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(123L, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("123");

        verify(productRepository, times(1)).findById(123L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_givenExistingId_deletesProduct() {
        Product existing = Product.builder()
                .id(70L)
                .name("Delete Me")
                .price(new BigDecimal("2.00"))
                .stock(3)
                .build();
        when(productRepository.findById(70L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(70L);

        verify(productRepository, times(1)).findById(70L);
        verify(productRepository, times(1)).delete(existing);
    }

    @Test
    void deleteProduct_givenMissingId_throwsNotFound() {
        when(productRepository.findById(701L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(701L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("701");

        verify(productRepository, times(1)).findById(701L);
        verify(productRepository, never()).delete(any(Product.class));
    }
}
