package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.serviceImpl.CartService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono
import java.net.URI

class CartMergeAuthenticationSuccessHandler(
    private val cartService: CartService
) : ServerAuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(CartMergeAuthenticationSuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        // Extract userId from the authentication principal
        val userId = authentication.name

        // Retrieve guestId from cookies
        val guestId = webFilterExchange.exchange.request.cookies["guestId"]?.firstOrNull()?.value

        // Merge guest cart into user's cart if guestId exists
        val mergeCartMono = if (guestId != null) {
            cartService.mergeCart(userId, guestId)
                .doOnSuccess {
                    logger.debug("Successfully merged cart for user $userId and guest $guestId")
                }
                .onErrorResume { error ->
                    logger.error("Failed to merge cart for user $userId: ${error.message}")
                    Mono.empty() // Ensure login is not blocked due to cart merge issues
                }
        } else {
            Mono.empty() // No guest cart to merge
        }

        // Redirect to the home page after merging the cart
        return mergeCartMono.then(
            Mono.defer {
                val response = webFilterExchange.exchange.response
                response.statusCode = HttpStatus.FOUND // HTTP 302 redirect
                response.headers.location = URI.create("/") // Redirect to home page
                response.setComplete() // Finalize the response
            }
        )
    }
}
