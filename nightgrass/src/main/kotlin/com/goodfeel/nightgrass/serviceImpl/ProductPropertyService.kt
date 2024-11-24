package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.repo.ProductPropertyRepository
import org.springframework.stereotype.Service

@Service
class ProductPropertyService(private val productPropertyRepository: ProductPropertyRepository) {

    fun getProductProperties(productId: Long) =
        productPropertyRepository.findByProductId(productId)

}
