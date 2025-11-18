package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.serviceImpl.CartService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
@RequestMapping("/api/cart")
class CartApiController(private val cartService: CartService) {

    private val logger = LoggerFactory.getLogger(CartApiController::class.java)

    @GetMapping("/updates", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamCartUpdates(): Flux<ServerSentEvent<Int>> {
        val heartbeat = Flux.interval(Duration.ofSeconds(15))
            .map { ServerSentEvent.builder<Int>().event("heartbeat").data(0).build() }
            .doOnNext {
                logger.debug("heartbeat is sent: ${it.data()}")
            }

        val updates = cartService.getCartUpdateStream()
            .map { updatedCount ->
                ServerSentEvent.builder<Int>().event("cart-update").data(updatedCount).build()
            }

        return Flux.merge(heartbeat, updates)
            .doOnSubscribe { logger.debug("SSE connection started") }
            .doOnCancel { logger.debug("SSE connection closed") }
            .doOnComplete { logger.debug("SSE connection completed") }
            .doOnError { error -> logger.error("SSE error occurred: ${error.message}", error) }

    }

    @PostMapping("/update")
    fun updateCart() {

    }

    @GetMapping("/my-cart")
    fun myCart() {
        // Implementation for retrieving the user's cart

    }

}
