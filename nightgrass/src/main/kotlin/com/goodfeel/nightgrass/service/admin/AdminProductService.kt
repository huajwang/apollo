package com.goodfeel.nightgrass.service.admin

import com.goodfeel.nightgrass.data.ProductPhoto
import com.goodfeel.nightgrass.data.admin.AdminProduct
import com.goodfeel.nightgrass.dto.admin.AdminProductRequest
import com.goodfeel.nightgrass.repo.ProductPhotoRepository
import com.goodfeel.nightgrass.repo.ProductPropertyRepository
import com.goodfeel.nightgrass.repo.admin.DiscountRepository
import com.goodfeel.nightgrass.repo.admin.AdminProductRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class AdminProductService(
    private val adminProductRepository: AdminProductRepository,
    private val discountRepository: DiscountRepository,
    private val propertyRepository: ProductPropertyRepository,
    private val photoRepository: ProductPhotoRepository
) {

    fun getAllProducts(): Flux<AdminProduct> = adminProductRepository.findAll()

    fun getProductById(id: Long): Mono<AdminProduct> = adminProductRepository.findById(id)

    fun createProduct(
        adminProductRequest: AdminProductRequest,
        fileUrls: List<String>
    ): Mono<AdminProduct> {
        val product = AdminProduct(
            productName = adminProductRequest.productName,
            description = adminProductRequest.description,
            imageUrl = adminProductRequest.imageUrl,
            price = adminProductRequest.price,

        )
        return adminProductRepository.save(product)
            .flatMap { savedProduct ->
                // map to a list of ProductPhoto. It is a light weight and fast operation.
                // No slow I/O involved. So, it is NON-Blocking
                // No need to wrap it in Mono.fromCallable to run on a separate thread.
                val productPhotos = fileUrls.map { filePath ->
                    ProductPhoto(
                        productId = savedProduct.productId!!,
                        photoUrl = filePath
                    )
                }
                photoRepository.saveAll(productPhotos).collectList().thenReturn(savedProduct)
            }
    }

    fun updateProduct(
        id: Long,
        adminProductRequest: AdminProductRequest,
        fileUrls: List<String>): Mono<AdminProduct> {
        return adminProductRepository.findById(id)
            .flatMap { existingProduct ->
                existingProduct.productName = adminProductRequest.productName
                existingProduct.description = adminProductRequest.description
                existingProduct.imageUrl = adminProductRequest.imageUrl
                existingProduct.price = adminProductRequest.price
                existingProduct.category = adminProductRequest.category
                existingProduct.setAdditionalInfoFromMap(adminProductRequest.additionalInfoMap)

                adminProductRepository.save(existingProduct)
            }.flatMap { savedProduct ->
                val productPhotos = fileUrls.map { filePath ->
                    ProductPhoto(
                        productId = savedProduct.productId!!,
                        photoUrl = filePath
                    )
                }
                photoRepository.saveAll(productPhotos).collectList().thenReturn(savedProduct)
            }
    }

    fun deleteProduct(id: Long): Mono<Void> = adminProductRepository.deleteById(id)
}
