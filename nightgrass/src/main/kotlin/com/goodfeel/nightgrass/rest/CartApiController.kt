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
        val guestSessionId = exchange.request.headers.getFirst("X-Guest-Session-Id")
        
        return authenticationUtility.extractUserFromExchangeOptional(exchange)
            .flatMap { user ->
                cartService.getCartForUserOrGuest(user, guestSessionId)
                    .flatMap { cart ->
                        // Process each cart item update in parallel
                        Flux.fromIterable(cartUpdateRequest.items)
                            .flatMap { updatedItem ->
                                if (updatedItem.itemId == null) {
                                    // Item has no ID - this is a new item to be added
                                    logger.debug("Adding new cart item for product: ${updatedItem.productId}")
                                    cartService.addCartItemToCart(cart, updatedItem)
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
                                // After all item updates, recalculate cart total
                                cartService.updateCartTotalByCartId(cart.cartId!!)
                                    .then(cartService.getCartItemsForCart(cart.cartId)
                                        .collectList()
                                        .map { updatedItems ->
                                            ResponseEntity.ok(CartResponse(items = updatedItems))
                                        }
                                    )
                            }
                    }
                    .doOnSuccess {
                        val userInfo = if (user != null) "authenticated user: ${user.oauthId}" else "guest"
                        logger.info("Cart updated successfully for $userInfo")
                    }
            }
            .onErrorResume { error ->
                logger.error("Failed to update cart", error)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }

    @GetMapping("/my-cart")
    fun myCart(exchange: ServerWebExchange): Mono<ResponseEntity<CartResponse>> {
        val guestSessionId = exchange.request.headers.getFirst("X-Guest-Session-Id")
        
        return authenticationUtility.extractUserFromExchangeOptional(exchange)
            .flatMap { user ->
                logger.debug("myCart called: user=${user?.oauthId}")
                
                cartService.getCartForUserOrGuest(user, guestSessionId)
                    .flatMap { cart ->
                        cartService.getCartItemsForCart(cart.cartId!!)
                            .collectList()
                            .map { cartItems ->
                                ResponseEntity.ok(CartResponse(items = cartItems))
                            }
                    }
                    .doOnSuccess {
                        val userInfo = if (user != null) "authenticated user: ${user.oauthId}" else "guest"
                        logger.info("Retrieved cart for $userInfo")
                    }
            }
            .onErrorResume { error ->
                logger.error("Failed to retrieve cart", error)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }

    @DeleteMapping("/clear")
    fun clearCart(exchange: ServerWebExchange): Mono<Void> {
        val guestSessionId = exchange.request.headers.getFirst("X-Guest-Session-Id")
        
        return authenticationUtility.extractUserFromExchangeOptional(exchange)
            .flatMap { user ->
                cartService.clearCart(user, guestSessionId)
                    .doOnSuccess {
                        val userInfo = if (user != null) "authenticated user: ${user.oauthId}" else "guest: $guestSessionId"
                        logger.info("Cart cleared successfully for $userInfo")
                    }
            }
            .onErrorResume { error ->
                logger.error("Failed to clear cart", error)
                Mono.error(error)
            }
    }

    @PostMapping("/merge")
    fun mergeGuestCartToUserCart(
        @RequestBody mergeRequest: MergeCartRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<CartResponse>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                // Only authenticated users can merge carts
                val guestSessionId: String = mergeRequest.guestSessionId
                if (guestSessionId.isBlank()) {
                    return@flatMap Mono.just(ResponseEntity.badRequest().build())
                }
                
                if (user.oauthId == null) {
                    return@flatMap Mono.just(ResponseEntity.badRequest().build())
                }

                cartService.mergeCart(user.oauthId, guestSessionId)
                    .then(cartService.getCartForUserOrGuest(user, null)
                        .flatMap { cart ->
                            cartService.getCartItemsForCart(cart.cartId!!)
                                .collectList()
                                .map { items ->
                                    logger.info("Cart merged successfully for user: ${user.oauthId} from guest: ${mergeRequest.guestSessionId}")
                                    ResponseEntity.ok(CartResponse(items = items))
                                }
                        }
                    )
            }
            .onErrorResume { error ->
                logger.error("Failed to merge cart", error)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }

}

data class MergeCartRequest(
    val guestSessionId: String
)
