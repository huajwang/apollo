package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.serviceImpl.CartService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.time.Duration

@RestController
class CartApiController(private val cartService: CartService) {

    private val logger = LoggerFactory.getLogger(CartApiController::class.java)

    @GetMapping("/cart/updates", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamCartUpdates(): Flux<ServerSentEvent<Int>> {
        logger.debug("cart updates controller gets invoked")
        val heartbeat = Flux.interval(Duration.ofSeconds(15))
            .map { ServerSentEvent.builder<Int>().event("heartbeat").data(0).build() }

        val updates = cartService.getCartUpdateStream()
            .map { updatedCount ->
                ServerSentEvent.builder<Int>().event("cart-update").data(updatedCount).build()
            }

        return Flux.merge(heartbeat, updates)
            .doOnSubscribe { logger.debug("SSE connection started") }
            .doOnCancel { logger.debug("SSE connection closed") }
            .doOnComplete { logger.debug("SSE connection completed") } // Log completions
            .doOnError { error -> logger.error("SSE error occurred: ${error.message}", error) } // Log errors

    }

}
