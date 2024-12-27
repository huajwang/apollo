package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.repo.ProductRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProcessedProductService(
    private val productRepository: ProductRepository
) {

    object ProductProcessor {
        fun process(dto: ProductDto): ProductDto {
            dto.processProductDto()
            return dto
        }
    }

    fun findAndProcessProductByProductId(productId: Long): Mono<ProductDto> =
        productRepository.findByProductId(productId)
            .map(ProductProcessor::process)

    fun findAllAndProcessProducts(): Flux<ProductDto> =
        productRepository.findAllProducts()
            .map(ProductProcessor::process)

    fun findTop8PopularOrNewProducts(): Flux<ProductDto> =
        productRepository.findTop8PopularOrNewProducts()
            .map(ProductProcessor::process)

    fun findTop3BigHits(): Flux<ProductDto> =
        productRepository.findTop3BigHits()
            .map(ProductProcessor::process)

}
