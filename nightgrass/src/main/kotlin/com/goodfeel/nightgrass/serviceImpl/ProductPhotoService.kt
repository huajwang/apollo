package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.dto.ProductPhotoDto
import com.goodfeel.nightgrass.repo.ProductPhotoRepo
import com.goodfeel.nightgrass.service.IProductPhotoService
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ProductPhotoService(private val productPhotoRepo: ProductPhotoRepo) : IProductPhotoService {

    override fun findProductImg(productId: Long): Flux<ProductPhotoDto> {
        return productPhotoRepo.findAllByProductId(productId).map<ProductPhotoDto> { productPhoto ->
            ProductPhotoDto( photoUrl = productPhoto.photoUrl)
        }
    }
}
