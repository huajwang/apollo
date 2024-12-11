package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.security.web.server.savedrequest.ServerRequestCache
import org.springframework.security.web.server.savedrequest.WebSessionServerRequestCache
import reactor.core.publisher.Mono
import java.net.URI

class CartMergeAuthenticationSuccessHandler(
    private val cartService: CartService,
    private val guestService: GuestService,
    private val orderService: OrderService,
    private val userService: UserService,
    private val requestCache: ServerRequestCache = WebSessionServerRequestCache()
) : ServerAuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(CartMergeAuthenticationSuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        // Extract userId from the authentication principal
        val userId = authentication.name

        val guestIdMono = guestService.getGuestId(webFilterExchange.exchange.request)

        // Merge guest cart into user's cart if guestId exists
        val mergeOperationsMono = guestIdMono.flatMap { guestId ->
            cartService.mergeCart(userId, guestId)
                .doOnSuccess {
                    logger.debug("Successfully merged cart for user $userId and guest $guestId")
                }
                .onErrorResume { error ->
                    logger.error("Failed to merge cart for user $userId: ${error.message}")
                    Mono.empty() // Ensure login is not blocked due to cart merge issues
                }
                .then(
                    orderService.mergeOrder(userId, guestId)
                        .doOnSuccess {
                            logger.debug("Successfully merged orders for user $userId and guest $guestId")
                        }
                        .onErrorResume { error ->
                            logger.error("Failed to merge orders for user $userId: ${error.message}")
                            Mono.empty() // Ensure login is not blocked due to order merge issues
                        }
                )
                .then(
                    userService.deleteUserByGuestId(guestId)
                        .doOnSuccess {
                            logger.debug("Deleted Guest from DB. guestId = $guestId")
                        }
                        .onErrorResume {
                            logger.debug("Failed to delete Guest from DB. guestId = $guestId")
                            Mono.empty()
                        }
                )
        }

        return mergeOperationsMono.then(
            requestCache.getRedirectUri(webFilterExchange.exchange) // Retrieve the original URL from the cache
                .defaultIfEmpty(URI.create("/")) // Default to home page if no target URL is saved
                .flatMap { targetUri ->
                    val response = webFilterExchange.exchange.response
                    response.statusCode = HttpStatus.FOUND // HTTP 302 redirect
                    response.headers.location = targetUri // Redirect to the original target page
                    response.setComplete() // Finalize the response
                }
        )
    }
}
