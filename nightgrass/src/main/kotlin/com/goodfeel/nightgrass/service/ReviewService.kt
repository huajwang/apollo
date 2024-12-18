package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.ProductReview
import com.goodfeel.nightgrass.repo.ReviewRepository
import com.goodfeel.nightgrass.web.util.ReviewContentRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository
) {

    fun getProductReview(productId: Long): Flux<ProductReview> =
        reviewRepository.findAllByProductId(productId)

    fun postReview(
        reviewer: String, productId: Long,
        reviewContentRequest: ReviewContentRequest
    ): Mono<Void> {
        val review = ProductReview(
            productId = productId,
            reviewer = reviewer,
            content = reviewContentRequest.reviewContent)
        return reviewRepository.save(review).then()
    }
}
