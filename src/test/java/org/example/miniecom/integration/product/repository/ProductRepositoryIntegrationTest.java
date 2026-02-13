package org.example.miniecom.integration.product.repository;

import org.example.miniecom.integration.support.TestcontainersConfig;
import org.example.miniecom.product.domain.Product;
import org.example.miniecom.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryIntegrationTest extends TestcontainersConfig {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void save_persistsProduct() {
        Product product = Product.builder()
                .name("Mouse")
                .price(new BigDecimal("25.50"))
                .stock(14)
                .build();

        Product saved = productRepository.saveAndFlush(product);
        Optional<Product> loaded = productRepository.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getName()).isEqualTo("Mouse");
        assertThat(loaded.get().getPrice()).isEqualByComparingTo("25.50");
        assertThat(loaded.get().getStock()).isEqualTo(14);
    }

    @Test
    void save_whenUpdatingExistingProduct_appliesNewValues() {
        Product created = productRepository.saveAndFlush(Product.builder()
                .name("Webcam")
                .price(new BigDecimal("70.00"))
                .stock(8)
                .build());

        created.setName("4K Webcam");
        created.setPrice(new BigDecimal("99.90"));
        created.setStock(5);
        Product updated = productRepository.saveAndFlush(created);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("4K Webcam");
        assertThat(updated.getPrice()).isEqualByComparingTo("99.90");
        assertThat(updated.getStock()).isEqualTo(5);
    }

    @Test
    void delete_removesProduct() {
        Product saved = productRepository.saveAndFlush(Product.builder()
                .name("Dock")
                .price(new BigDecimal("79.00"))
                .stock(3)
                .build());
        Long productId = saved.getId();

        productRepository.deleteById(productId);
        productRepository.flush();

        assertThat(productRepository.findById(productId)).isEmpty();
    }
}
