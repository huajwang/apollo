package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.Product
import com.goodfeel.nightgrass.dto.ProductDto
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IProductService {
    fun allProducts(): Flux<ProductDto>
    fun getProductById(id: Long): Mono<ProductDto>
    fun saveProduct(product: Product): Mono<Product>
    fun deleteProduct(id: Long): Mono<Void>
    fun getTop3BigHits(): Flux<ProductDto>
    fun getTop8PopularOrNewProducts(): Flux<ProductDto>
}
