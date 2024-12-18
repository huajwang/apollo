package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.service.ReviewService
import com.goodfeel.nightgrass.web.util.ReviewContentRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.security.Principal

@RestController
@RequestMapping("/review")
class ReviewController(
    private val reviewService: ReviewService
) {

    @PostMapping("/{productId}")
    fun postReview(
        principal: Principal,
        @PathVariable("productId") productId: Long,
        @ModelAttribute("reviewContent") reviewContentRequest: ReviewContentRequest,
    ): Mono<ResponseEntity<Map<String, Any>>> {
        return reviewService.postReview(principal.name, productId, reviewContentRequest)
            .thenReturn(ResponseEntity.ok(mapOf<String, Any>("success" to true)))
            .onErrorResume { e ->
                Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(mapOf("error" to (e.message ?: "Unexpected error occurs")))
                )
            }
    }
}
