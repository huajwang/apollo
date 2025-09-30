package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.ProductVideo
import com.goodfeel.nightgrass.repo.ProductVideoRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ProductVideoService (
    private val productVideoRepository: ProductVideoRepository
) {
    fun getProductVideos(productId: Long): Flux<ProductVideo> =
        productVideoRepository.findAllByProductId(productId)
            .sort(Comparator.comparingInt { it.orderIndex })
}
