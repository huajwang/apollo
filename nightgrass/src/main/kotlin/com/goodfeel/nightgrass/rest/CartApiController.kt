package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.util.AuthenticationUtility
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux

data class CartUpdateRequest(val items: List<CartItemDto>)
data class CartResponse(val items: List<CartItemDto>)

@RestController
@RequestMapping("/api/cart")
class CartApiController(
    private val cartService: CartService,
    private val authenticationUtility: AuthenticationUtility
) {

    private val logger = LoggerFactory.getLogger(CartApiController::class.java)

    @PostMapping("/update")
    fun updateCart(
        @RequestBody cartUpdateRequest: CartUpdateRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CartResponse>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                cartService.getCartForUserOrGuest(user)
                    .flatMap { cart ->
                        // Process each cart item update in parallel
                        Flux.fromIterable(cartUpdateRequest.items)
                            .flatMap { updatedItem ->
                                if (updatedItem.itemId == null) {
                                    // Item has no ID - skip (malformed request)
                                    logger.warn("Received cart item without ID, skipping")
                                    Mono.empty<Long>()
                                } else if (updatedItem.quantity <= 0) {
                                    // Quantity 0 or negative - remove item
                                    logger.debug("Removing cart item: ${updatedItem.itemId}")
                                    cartService.removeCartItemFromCart(updatedItem.itemId)
                                } else {
                                    // Positive quantity - update quantity
                                    logger.debug("Updating cart item ${updatedItem.itemId} quantity to ${updatedItem.quantity}")
                                    cartService.updateQuantity(updatedItem.itemId, updatedItem.quantity)
                                }
                            }
                            .collectList() // Wait for all updates to complete
                            .flatMap {
                                // After updates, fetch updated cart items and return
                                cartService.getCartItemsForCart(cart.cartId!!)
                                    .collectList()
                                    .map { updatedItems ->
                                        ResponseEntity.ok(CartResponse(items = updatedItems))
                                    }
                            }
                    }
                    .doOnSuccess {
                        logger.info("Cart updated successfully for user: ${user.oauthId}")
                    }
            }
            .onErrorResume { error ->
                logger.error("Failed to update cart", error)
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
            }
    }

    @GetMapping("/my-cart")
    fun myCart(exchange: ServerWebExchange): Mono<ResponseEntity<CartResponse>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                cartService.getCartForUserOrGuest(user)
                    .flatMap { cart ->
                        cartService.getCartItemsForCart(cart.cartId!!)
                            .collectList()
                            .map { cartItems ->
                                ResponseEntity.ok(CartResponse(items = cartItems))
                            }
                    }
                    .doOnSuccess { logger.info("Retrieved cart for user: ${user.oauthId}") }
            }
            .onErrorResume { error ->
                logger.error("Failed to retrieve cart", error)
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
            }
    }

    @DeleteMapping("/clear")
    fun clearCart(exchange: ServerWebExchange): Mono<Void> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                cartService.clearCart(user)
                    .doOnSuccess { logger.info("Cart cleared successfully for user: ${user.oauthId}") }
            }
            .onErrorResume { error ->
                logger.error("Failed to clear cart", error)
                Mono.error(error)
            }
    }

}
