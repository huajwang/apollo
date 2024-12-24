package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.dto.ProductPhotoDto
import com.goodfeel.nightgrass.repo.ProductPhotoRepository
import com.goodfeel.nightgrass.service.IProductPhotoService
import com.goodfeel.nightgrass.web.util.Utility
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ProductPhotoService(
    private val productPhotoRepository: ProductPhotoRepository
) : IProductPhotoService {

    override fun findProductImg(productId: Long): Flux<ProductPhotoDto> {
        return productPhotoRepository.findAllByProductId(productId).map { productPhoto ->
            ProductPhotoDto( photoUrl = Utility.generateMediaUrl(productPhoto.photoUrl))
        }
    }
}
