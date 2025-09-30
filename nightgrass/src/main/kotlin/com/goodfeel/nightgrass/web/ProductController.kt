package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.VideoType
import com.goodfeel.nightgrass.service.ReviewService
import com.goodfeel.nightgrass.serviceImpl.ProductPhotoService
import com.goodfeel.nightgrass.serviceImpl.ProductPropertyService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import com.goodfeel.nightgrass.serviceImpl.ProductVideoService
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
    private val productVideoService: ProductVideoService,
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
        val productDtoMono = productService.getProductById(productId)
        val productPhotosFlux = productPhotoService.findProductImg(productId)
        val productPropertyFlux = productPropertyService.getProductProperties(productId)

        val productVideoFlux = productVideoService.getProductVideos(productId).cache()
        // Categorize videos into "playable" and "iframe-embed" types
        val fileVideosFlux = productVideoFlux.filter {
            it.videoType == VideoType.FILE || it.videoUrl.endsWith(".mp4")
        }.map {
            it.videoUrl
        }
        val youtubeVideosFlux = productVideoFlux.filter {
            it.videoType == VideoType.YOUTUBE
                    || it.videoUrl.contains("youtu.be") || it.videoUrl.contains("youtube.com")
        }.map {
            it.videoUrl
        }

        val vimeoVideosFlux = productVideoFlux.filter {
            it.videoType == VideoType.VIMEO || it.videoUrl.contains("vimeo.com")
        }.map {
            it.videoUrl
        }

        val reviews = reviewService.getProductReview(productId).collectList()

        return Mono.zip(productDtoMono,
            productPhotosFlux.collectList(),
            productPropertyFlux.collectList(),
            fileVideosFlux.collectList(),
            youtubeVideosFlux.collectList(),
            vimeoVideosFlux.collectList(),
            reviews)
            .map { tuple ->
                val productDto = tuple.t1
                val productPhotos = tuple.t2
                val productProperties = tuple.t3
                val fileVideos = tuple.t4
                val youtubeVideos = tuple.t5
                val vimeoVideos = tuple.t6
                val productReviews = tuple.t7

                model.addAttribute("product", productDto)
                model.addAttribute("productPhotos", productPhotos)
                model.addAttribute("productProperties", productProperties)
                model.addAttribute("fileVideos", fileVideos)
                model.addAttribute("youtubeVideos", youtubeVideos)
                model.addAttribute("vimeoVideos", vimeoVideos)
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
