package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.dto.ProductPhotoDto
import com.goodfeel.nightgrass.repo.ProductPhotoRepository
import com.goodfeel.nightgrass.service.AliyunOssService
import com.goodfeel.nightgrass.service.IProductPhotoService
import com.goodfeel.nightgrass.web.util.Utility
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProductPhotoService(
    private val productPhotoRepository: ProductPhotoRepository,
    private val aliyunOssService: AliyunOssService
) : IProductPhotoService {

    override fun findProductImg(productId: Long): Flux<ProductPhotoDto> {
        return productPhotoRepository.findAllByProductId(productId).map { productPhoto ->
            ProductPhotoDto( photoUrl = Utility.generateMediaUrl(productPhoto.photoUrl))
        }
    }

    override fun deleteProductPhotos(productId: Long): Mono<Void> {
        return productPhotoRepository.findAllByProductId(productId)
            .map { productPhoto ->
                productPhoto.photoUrl
            }
            .collectList()
            .flatMap { photoUrls ->
                aliyunOssService.deletePhotosOnOss(photoUrls)
            }
            // If OSS deletion fails, the database records are not deleted, ensuring consistency.
            .then(
                productPhotoRepository.deleteByProductId(productId)
            )
    }
}
