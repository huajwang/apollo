package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ProductProperty
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface ProductPropertyRepository : ReactiveCrudRepository<ProductProperty, Long> {
    fun findByProductId(productId: Long): Flux<ProductProperty>
}
