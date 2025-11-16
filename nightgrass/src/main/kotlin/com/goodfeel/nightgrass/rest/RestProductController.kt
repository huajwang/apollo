package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.dto.ProductDto
import com.goodfeel.nightgrass.service.ReviewService
import com.goodfeel.nightgrass.serviceImpl.ProductPhotoService
import com.goodfeel.nightgrass.serviceImpl.ProductPropertyService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/product")
class RestProductController(
    private val productService: ProductService,
    private val productPhotoService: ProductPhotoService,
    private val productPropertyService: ProductPropertyService,
    private val reviewService: ReviewService
) {

    @GetMapping("/all")
    fun listProducts(): Flux<ProductDto> {
        return productService.allProducts()
    }

    @GetMapping("/detail/{id}")
    fun productDetail(@PathVariable("id") productId: Long): Mono<Map<String, Any>> {
        val productDtoMono = productService.getProductById(productId)
        val productPhotosFlux = productPhotoService.findProductImg(productId)
        val productPropertyFlux = productPropertyService.getProductProperties(productId)

        val reviewFlux = reviewService.getProductReview(productId)

        return Mono.zip(
            productDtoMono,
            productPhotosFlux.collectList(),
            productPropertyFlux.collectList(),
            reviewFlux.collectList()
        ).map { tuple ->
            mapOf(
                "product" to tuple.t1,
                "gallery" to tuple.t2,
                "specifications" to tuple.t3,
                "reviews" to tuple.t4
            )
        }
    }

    @GetMapping("/related/{id}")
    fun relatedProducts(@PathVariable("id") productId: Long): Flux<ProductDto> {
        return productService.getRelatedProducts(productId)
    }
}

