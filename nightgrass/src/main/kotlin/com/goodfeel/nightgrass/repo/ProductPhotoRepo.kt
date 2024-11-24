package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ProductPhoto
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface ProductPhotoRepo : ReactiveCrudRepository<ProductPhoto, Long> {
    fun findAllByProductId(productId: Long): Flux<ProductPhoto>
}
