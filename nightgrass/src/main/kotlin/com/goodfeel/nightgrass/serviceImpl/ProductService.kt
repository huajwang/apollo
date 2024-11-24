package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.repo.ProductRepository
import com.goodfeel.nightgrass.service.IProductService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProductService(private val productRepository: ProductRepository) : IProductService {

    override fun allProducts(): Flux<ProductDto> {
        return productRepository.findAll().map { product: Product ->
            ProductDto(
                productId = product.productId ?: throw IllegalArgumentException("Product ID cannot be null"),
                productName = product.productName,
                description = product.description,
                imageUrl = product.imageUrl,
                price = product.price
            )
        }
    }

    override fun getProductById(id: Long): Mono<ProductDto> {
        return productRepository.findById(id).map { product: Product ->
            ProductDto(
                productId = product.productId ?: throw IllegalArgumentException("productId cannot be null"),
                productName = product.productName,
                description = product.description,
                imageUrl = product.imageUrl,
                price = product.price
            )
        }.switchIfEmpty(Mono.error(NoSuchElementException("Product with ID $id not found")))
    }

    override fun saveProduct(product: Product): Mono<Product> {
        return productRepository.save(product)
    }

    override fun deleteProduct(id: Long): Mono<Void> {
        return productRepository.deleteById(id)
    }
}
