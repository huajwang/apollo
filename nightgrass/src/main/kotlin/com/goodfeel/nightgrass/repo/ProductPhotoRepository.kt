package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.ProductPhoto
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductPhotoRepository : ReactiveCrudRepository<ProductPhoto, Long> {
    fun findAllByProductId(productId: Long): Flux<ProductPhoto>
    fun deleteByProductId(productId: Long): Mono<Void>

    @Query("DELETE FROM e_mall_product_photo WHERE product_id = :productId AND photo_url IN (:photoUrls)")
    fun deletePhotoUrls(productId: Long, photoUrls: List<String>): Mono<Void>

}
