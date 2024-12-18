package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ProductReview
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface ReviewRepository: ReactiveCrudRepository<ProductReview, Long> {
    fun findAllByProductId(productId: Long): Flux<ProductReview>
}
