package com.goodfeel.nightgrass.rest.admin

import com.goodfeel.nightgrass.exception.ForeignKeyConstraintViolationException
import com.goodfeel.nightgrass.service.AliyunOssService
import com.goodfeel.nightgrass.service.admin.AdminProductService
import com.goodfeel.nightgrass.serviceImpl.ProductPhotoService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import com.goodfeel.nightgrass.web.util.Utility
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/admin")
class AdminApiController(
    private val productService: ProductService,
    private val productPhotoService: ProductPhotoService,
    private val aliyunOssService: AliyunOssService,
    private val adminProductService: AdminProductService
) {

    @DeleteMapping("/products/{productId}")
    fun deleteProduct(@PathVariable productId: Long): Mono<ResponseEntity<Void>> {
        return productPhotoService.deleteProductPhotos(productId)
            .then(productService.deleteProduct(productId))
            .then(Mono.just(ResponseEntity.ok().build<Void>()))
            .onErrorResume { error ->
                when (error) {
                    is ForeignKeyConstraintViolationException -> {
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                    }
                    else -> {
                        Mono.just(ResponseEntity.notFound().build())
                    }
                }
            }
    }

    @PostMapping("/products/photos/delete")
    fun deleteProductPhotos(@RequestBody request: PhotoDeletionRequest): Mono<ResponseEntity<Void>> {
        if (request.photoUrls.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build())
        }
        return aliyunOssService.deletePhotosOnOss(request.photoUrls)
            .then(
                adminProductService.deletePhotosFromDb(request.productId, Utility.removeOssPrefix(request.photoUrls))
            )
            .then(
                Mono.just(ResponseEntity.ok().build<Void>())
            )
            .onErrorResume {
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }

    data class PhotoDeletionRequest(
        val productId: Long,
        val photoUrls: List<String>
    )

}
