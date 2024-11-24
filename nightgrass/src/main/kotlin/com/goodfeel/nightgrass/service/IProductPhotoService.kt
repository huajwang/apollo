package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.dto.ProductPhotoDto
import reactor.core.publisher.Flux

interface IProductPhotoService {
    fun findProductImg(productId: Long): Flux<ProductPhotoDto>
}
