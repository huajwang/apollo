package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ProductVideo
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface ProductVideoRepository: ReactiveCrudRepository<ProductVideo, Long> {
    fun findAllByProductId(productId: Long): Flux<ProductVideo>
}
