package com.goodfeel.nightgrass.serviceImpl;

import com.goodfeel.nightgrass.data.Product;
import com.goodfeel.nightgrass.repo.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import java.math.BigDecimal;

public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this); // Initialize mocks
    }

    @Test
    public void allProducts() {
        // Arrange
        Product product1 = new Product(1L, "Product 1", "Description 1",
                "image1.jpg", BigDecimal.valueOf(100.00));
        Product product2 = new Product(2L, "Product 2", "Description 2",
                "image2.jpg", BigDecimal.valueOf(200.00));
        when(productRepository.findAll()).thenReturn(Flux.just(product1, product2));

        StepVerifier.create(productService.allProducts())
                .expectNextMatches(productDto -> productDto.getProductName().equals("Product 1"))
                .expectNextMatches(productDto -> productDto.getProductName().equals("Product 2"))
                .verifyComplete();
    }

    @Test
    public void getProductById_shouldReturnProductWhenExists() {
        // Arrange
        Long productId = 1L;
        Product product = new Product(productId, "Product 1", "Description 1",
                "image1.jpg", BigDecimal.valueOf(100.00));
        when(productRepository.findById(productId)).thenReturn(Mono.just(product));

        StepVerifier.create(productService.getProductById(productId))
                .expectNextMatches(productDto -> productDto.getProductId() == productId &&
                        productDto.getProductName().equals("Product 1"))
                .verifyComplete();
    }

    @Test
    public void getProductById_shouldReturnEmptyWhenNotExists() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Mono.empty());
        StepVerifier.create(productService.getProductById(productId))
                .verifyComplete(); // Verifies that the Mono completes without emitting any items
    }

    @Test
    public void saveProduct_shouldSaveAndReturnProduct() {
        Product product = new Product(null, "New Product", "New Description",
                "newimage.jpg", BigDecimal.valueOf(150.00));
        Product savedProduct = new Product(1L, "New Product", "New Description",
                "newimage.jpg", BigDecimal.valueOf(150.00));
        when(productRepository.save(product)).thenReturn(Mono.just(savedProduct));

        StepVerifier.create(productService.saveProduct(product))
                .expectNext(savedProduct)
                .verifyComplete();
    }

    @Test
    public void deleteProduct_shouldDeleteProduct() {
        Long productId = 1L;
        when(productRepository.deleteById(productId)).thenReturn(Mono.empty());
        StepVerifier.create(productService.deleteProduct(productId))
                .verifyComplete(); // Verifies that the deletion completes without errors
    }

}
