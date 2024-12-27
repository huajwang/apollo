package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.dto.ProductVideo
import com.goodfeel.nightgrass.service.ReviewService
import com.goodfeel.nightgrass.serviceImpl.ProductPhotoService
import com.goodfeel.nightgrass.serviceImpl.ProductPropertyService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/product")
class ProductController(
    private val productService: ProductService,
    private val productPhotoService: ProductPhotoService,
    private val productPropertyService: ProductPropertyService,
    private val reviewService: ReviewService
) {

    @GetMapping("/all")
    fun listProducts(model: Model): Mono<String> {
        return productService.allProducts().collectList()
            .doOnNext { model.addAttribute("products", it) }
            .thenReturn("product-list")
    }

    @GetMapping("/detail")
    fun productDetail(@RequestParam("id") productId: Long, model: Model): Mono<String> {
        val productVideos = listOf(
            ProductVideo("e88.mp4"),
            ProductVideo("E99K3-china.mp4"),
            ProductVideo("E99K3-eng.mp4")
        )

        val productDtoMono = productService.getProductById(productId)
        val productPhotosFlux = productPhotoService.findProductImg(productId)
        val productPropertyFlux = productPropertyService.getProductProperties(productId)
        val reviews = reviewService.getProductReview(productId).collectList()

        return Mono.zip(productDtoMono,
            productPhotosFlux.collectList(),
            productPropertyFlux.collectList(),
            reviews)
            .map { tuple ->
                // Explicitly access Tuple3 components
                val productDto = tuple.t1
                val productPhotos = tuple.t2
                val productProperties = tuple.t3
                val productReviews = tuple.t4

                // Add attributes to the model
                model.addAttribute("product", productDto)
                model.addAttribute("productPhotos", productPhotos)
                model.addAttribute("productVideos", productVideos)
                model.addAttribute("productProperties", productProperties)
                model.addAttribute("reviews", productReviews)

                "product-detail"
            }
            .onErrorResume { e ->
                model.addAttribute("error",
                    "An error occurred while loading product details: ${e.message}")
                Mono.just("/error/error-page")
            }
    }

}
