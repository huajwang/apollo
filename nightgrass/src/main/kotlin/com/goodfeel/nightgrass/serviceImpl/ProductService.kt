package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.exception.ForeignKeyConstraintViolationException
import com.goodfeel.nightgrass.repo.ProductRepository
import com.goodfeel.nightgrass.service.IProductService
import com.goodfeel.nightgrass.service.ProcessedProductService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val processedProductService: ProcessedProductService
) : IProductService {

    override fun allProducts(): Flux<ProductDto> {
        return processedProductService.findAllAndProcessProducts()
            .onErrorResume { ex ->
                Flux.error(RuntimeException("Failed to fetch all products: ${ex.message}", ex))
            }
    }

    override fun getProductById(id: Long): Mono<ProductDto> {
        return processedProductService.findAndProcessProductByProductId(id)
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
            .onErrorMap { ex ->
                if (ex.message?.contains("foreign key") == true) {
                    ForeignKeyConstraintViolationException(ex.message!!)
                } else {
                    RuntimeException("Failed to delete product with ID $id: ${ex.message}", ex)
                }

            }
    }

    override fun getTop3BigHits(): Flux<ProductDto> {
        return processedProductService.findTop3BigHits()
    }

    override fun getTop8PopularOrNewProducts(): Flux<ProductDto> =
        processedProductService.findTop8PopularOrNewProducts()

}
