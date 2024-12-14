package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.repo.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.dao.DataAccessException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal

class ProductServiceTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productRepository = mock(ProductRepository::class.java)
        productService = ProductService(productRepository)
    }

    @Test
    fun `allProducts should return list of ProductDto`() {
        val products = listOf(
            Product(1L, "Product 1", "Description 1",
                "image1.jpg", BigDecimal.valueOf(10)),
            Product(2L, "Product 2", "Description 2",
                "image2.jpg", BigDecimal.valueOf(20))
        )
        `when`(productRepository.findAll()).thenReturn(Flux.fromIterable(products))

        StepVerifier.create(productService.allProducts())
            .expectNextMatches { it.productId == 1L && it.productName == "Product 1" }
            .expectNextMatches { it.productId == 2L && it.productName == "Product 2" }
            .verifyComplete()
    }

    @Test
    fun `allProducts should handle error`() {
        `when`(productRepository.findAll())
            .thenReturn(Flux.error(RuntimeException("Database error")))

        StepVerifier.create(productService.allProducts())
            .expectErrorMatches { it is RuntimeException && it.message!!.contains("Failed to fetch all products") }
            .verify()
    }

    @Test
    fun `getProductById should return ProductDto for valid ID`() {
        val product = Product(1L, "Product 1", "Description 1",
            "image1.jpg", BigDecimal.valueOf(10))
        `when`(productRepository.findById(1L)).thenReturn(Mono.just(product))

        StepVerifier.create(productService.getProductById(1L))
            .expectNextMatches { it.productId == 1L && it.productName == "Product 1" }
            .verifyComplete()
    }

    @Test
    fun `getProductById should handle missing product`() {
        `when`(productRepository.findById(1L)).thenReturn(Mono.empty())

        StepVerifier.create(productService.getProductById(1L))
            .expectErrorMatches {
                it is NoSuchElementException && it.message!!.contains("Product with ID 1 not found") }
            .verify()
    }

    @Test
    fun `getProductById should handle repository error`() {
        `when`(productRepository.findById(1L))
            .thenReturn(Mono.error(RuntimeException("Database error")))

        StepVerifier.create(productService.getProductById(1L))
            .expectErrorMatches { it is RuntimeException && it.message!!.contains("Error fetching product with ID 1") }
            .verify()
    }

    @Test
    fun `saveProduct should save and return the product`() {
        val product = Product(1L, "Product 1", "Description 1",
            "image1.jpg", BigDecimal.valueOf(10.0))
        `when`(productRepository.save(product)).thenReturn(Mono.just(product))

        StepVerifier.create(productService.saveProduct(product))
            .expectNextMatches { it.productId == 1L && it.productName == "Product 1" }
            .verifyComplete()
    }

    @Test
    fun `saveProduct should handle error`() {
        val product = Product(1L, "Product 1", "Description 1",
            "image1.jpg", BigDecimal.valueOf(10.0))
        `when`(productRepository.save(product))
            .thenReturn(Mono.error(RuntimeException("Database error")))

        StepVerifier.create(productService.saveProduct(product))
            .expectErrorMatches { it is RuntimeException && it.message!!.contains("Failed to save product") }
            .verify()
    }

    @Test
    fun `deleteProduct should complete successfully`() {
        `when`(productRepository.deleteById(1L)).thenReturn(Mono.empty())

        StepVerifier.create(productService.deleteProduct(1L))
            .verifyComplete()

        verify(productRepository).deleteById(1L)
    }

    @Test
    fun `deleteProduct should handle error`() {
        `when`(productRepository.deleteById(1L))
            .thenReturn(Mono.error(RuntimeException("Database error")))

        StepVerifier.create(productService.deleteProduct(1L))
            .expectErrorMatches {
                it is RuntimeException && it.message!!.contains("Failed to delete product with ID 1") }
            .verify()
    }
}
