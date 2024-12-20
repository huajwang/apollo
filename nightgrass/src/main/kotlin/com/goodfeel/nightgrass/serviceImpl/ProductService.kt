package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.repo.ProductRepository
import com.goodfeel.nightgrass.service.IProductService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProductService(
    private val productRepository: ProductRepository
) : IProductService {

    override fun allProducts(): Flux<ProductDto> {
        return productRepository.findAllProducts()
            .map { productDto ->
                productDto.processProductDto()
                productDto
            }
            .onErrorResume { ex ->
                Flux.error(RuntimeException("Failed to fetch all products: ${ex.message}", ex))
            }
    }

    override fun getProductById(id: Long): Mono<ProductDto> {
        return productRepository.findByProductId(id)
            .map { productDto ->
                productDto.processProductDto()
                productDto
            }
            .onErrorResume { ex ->
                Mono.error(RuntimeException("Error fetching product with ID $id: ${ex.message}", ex))
            }
            .switchIfEmpty(Mono.error(NoSuchElementException("Product with ID $id not found")))

    }

    override fun saveProduct(product: Product): Mono<Product> {
        return productRepository.save(product)
            .onErrorResume { ex ->
                Mono.error(RuntimeException("Failed to save product: ${ex.message}", ex))
            }
    }

    override fun deleteProduct(id: Long): Mono<Void> {
        return productRepository.deleteById(id)
            .onErrorResume { ex ->
                Mono.error(RuntimeException("Failed to delete product with ID $id: ${ex.message}", ex))
            }
    }

    override fun getTop3BigHits(): Flux<ProductDto> {
        return productRepository.findTop3BigHits()
            .map { productDto ->
                productDto.processProductDto()
                productDto
            }
    }

    override fun getTop8PopularOrNewProducts(): Flux<ProductDto> {
        return productRepository.findTop8PopularOrNewProducts()
            .map { productDto ->
                productDto.processProductDto()
                productDto
            }
    }

    private fun Product.toDto(): ProductDto {
        return ProductDto(
            productId = this.productId ?: throw IllegalArgumentException("Product ID cannot be null"),
            productName = this.productName,
            description = this.description,
            imageUrl = this.imageUrl,
            price = this.price,
            additionalInfoMap = this.getAdditionalInfoAsMap(),
            category = this.category
        )
    }
}
