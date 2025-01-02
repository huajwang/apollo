package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.dto.ProductPhotoDto
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IProductPhotoService {
    fun findProductImg(productId: Long): Flux<ProductPhotoDto>
    fun deleteProductPhotos(productId: Long): Mono<Void>
}
