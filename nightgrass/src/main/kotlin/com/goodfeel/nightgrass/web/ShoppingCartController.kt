package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.web.util.CartItemUpdateRequest
import com.goodfeel.nightgrass.web.util.AddCartRequest
import com.goodfeel.nightgrass.web.util.RemoveCartRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.security.Principal

@Controller
@RequestMapping("/cart")
class ShoppingCartController(
    private val cartService: CartService,
    private val guestService: GuestService
) {
    private val logger: Logger = LoggerFactory.getLogger(ShoppingCartController::class.java)

    @GetMapping
    fun viewCart(
        model: Model,
        principal: Principal?, // Null for guests
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Mono<String> {
        // cache the cart
        val cartMono = guestService.retrieveUserGuestOrCreate(principal, request, response)
            .flatMap { user ->
                cartService.getCartForUserOrGuest(user)
                    .doOnSuccess { cart -> logger.debug("Cart fully saved and retrieved: $cart") }
                    .cache()
            }

        val cartItemDtosFlux = cartMono.flatMapMany { cart ->
            cartService.getCartItemsForCart(cart.cartId!!)
                .map { cartItemDto ->
                    cartItemDto.processProperties()
                    cartItemDto
                }
        }

        val subTotalMono = cartMono.flatMap { cart ->
            cartService.getSubtotal(cart.cartId!!)
        }

        val savingsMono = cartMono.flatMap { cart ->
            cartService.getSavings(cart.cartId!!)
        }

        val totalAfterDiscountMono = cartMono.flatMap { cart ->
            cartService.getTotalAfterDiscount(cart.cartId!!)
        }

        return Mono.zip(cartItemDtosFlux.collectList(), subTotalMono, savingsMono, totalAfterDiscountMono)
            .map { tuple ->
                val cartItems = tuple.t1
                val subtotal = tuple.t2
                val savings = tuple.t3
                val totalAfterDiscount = tuple.t4
                model.addAttribute("cartItems", cartItems)
                model.addAttribute("subtotal", subtotal)
                model.addAttribute("savings", savings)
                model.addAttribute("cartTotal", totalAfterDiscount)
                "shopping-cart" // Return the view name
            }
            .onErrorResume { e ->
                logger.error("Error retrieving cart: ${e.message}", e)
                model.addAttribute("error", "Failed to load cart")
                Mono.just("error")
            }
    }

    @PostMapping("/add")
    fun addToCart(
        @RequestBody addCartRequest: AddCartRequest,
        principal: Principal?, // Null for guest users
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Adding product ${addCartRequest.productId} with properties: ${addCartRequest.properties}")
        return guestService.retrieveUserGuestOrCreate(principal, request, response)
            .flatMap { user ->
                cartService.addProductToCart(addCartRequest, user)
                    .flatMap { _ -> cartService.getCartItemCount(user) }
                    .map { count ->
                        ResponseEntity.ok(mapOf<String, Any>("cartItemCount" to count))
                    }
            }
            .onErrorResume { e ->
                logger.error("Failed to add product to cart: ${e.message}", e)
                Mono.just(ResponseEntity.badRequest().body(mapOf("error" to "Failed to add product to cart")))
            }
    }

    @PostMapping("/remove")
    fun removeFromCart(
        @RequestBody removeCartRequest: RemoveCartRequest
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Remove cartItem: ${removeCartRequest.itemId}")
        return cartService.removeCartItemFromCart(removeCartRequest.itemId)
            .flatMap { cartId ->
                cartService.getCartItemsForCart(cartId)
                    .doOnNext { item -> logger.debug("Emitted cart item: $item") } // Log each item emitted
                    .collectList()
                    .doOnNext { list -> logger.debug("Collected cart items into list: $list") }
                    .map { cartItems ->
                        val responseBody = mapOf(
                            "error" to false,
                            "items" to cartItems
                        )
                        logger.debug("Response body: $responseBody")
                        ResponseEntity.ok(responseBody)
                    }
            }
            .switchIfEmpty(
                Mono.just(
                    ResponseEntity.ok(
                        mapOf(
                            "error" to false,
                            // Explicitly include an empty list to avoid empty response body
                            "items" to emptyList<CartItemDto>()
                        )
                    )
                )
            )
            .onErrorResume { error ->
                logger.error("Error removing item ${removeCartRequest.itemId}: ${error.message}")
                Mono.just(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        mapOf(
                            "message" to "Failed to remove the item",
                            "error" to true
                        )
                    )
                )
            }
    }

    @PostMapping("/update-quantity")
    fun updateQuantity(
        @RequestBody updateRequest: CartItemUpdateRequest
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Received updateRequest request body from front end ajax call: {}", updateRequest)
        return cartService.updateQuantity(updateRequest.itemId, updateRequest.quantity!!)
            .flatMap { cartItem ->
                cartService.getCartItemsForCart(cartItem.cartId).collectList()
                    .map { cartItems ->
                        ResponseEntity.ok(mapOf<String, Any>(
                            "message" to "Quantity updated",
                            "items" to cartItems)
                        )
                    }
            }
            .onErrorResume { e ->
                // Handle specific exceptions with tailored responses
                val errorResponse = mapOf(
                    "message" to "Failed to update quantity",
                    "error" to (e.message ?: "Unknown error on update quantity")
                )
                Mono.just(ResponseEntity.badRequest().body(errorResponse))
            }
    }

    @PostMapping("/update-total-on-checkbox")
    fun updateTotal(
        @RequestBody request: CartItemUpdateRequest
    ): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("updateTotal on checkbox update: $request")
        return cartService.updateCartTotalAndNotifyCartUpdate(request.itemId, request.isChecked!!)
            .flatMap { cart ->
                cartService.getCartItemsForCart(cart.cartId!!).collectList()
            }
            .map { cartItems ->
                ResponseEntity.ok(mapOf<String, Any>("items" to cartItems))
            }
    }

    /**
     * When customer press Checkout button on shopping cart page, front end javascript post a request
     * to create order, insert order items and clean up shopping cart.
     * @return
     */
    @PostMapping("/checkout")
    fun checkout(
        principal: Principal?,
        httpRequest: ServerHttpRequest,
        httpResponse: ServerHttpResponse
    ): Mono<ResponseEntity<Map<String, Long?>>> {
        val userMono = guestService.retrieveUserGuestOrCreate(principal, httpRequest, httpResponse)
        return userMono.flatMap { user ->
            cartService.createOrderAndCleanupShoppingCart(user.oauthId, user.guestId)
                .map { order: Order ->
                    ResponseEntity.ok(mapOf("orderId" to order.orderId))
                }
                .onErrorResume { e ->
                    logger.error("Error occurs on shopping cart checkout: $e")
                    Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(mapOf("orderId" to null))
                    )
                }
        }
    }

    @GetMapping("/total")
    fun getCartTotal(
        principal: Principal?, // Null for guests
        httpRequest: ServerHttpRequest,
        httpResponse: ServerHttpResponse): Mono<ResponseEntity<BigDecimal>> {
        val userMono = guestService.retrieveUserGuestOrCreate(principal, httpRequest, httpResponse)
        return userMono.flatMap { user ->
            cartService.getCartForUserOrGuest(user)
                .flatMap { cart ->
                    cartService.getTotalAfterDiscount(cart.cartId!!)
                        .map { body -> ResponseEntity.ok(body) }
                        .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(BigDecimal.ZERO))
                }
        }
    }

    @GetMapping("/items")
    fun getCartItems(
        principal: Principal?,
        httpRequest: ServerHttpRequest,
        httpResponse: ServerHttpResponse
    ): Mono<ResponseEntity<Map<String, Any>>> {
        val userMono = guestService.retrieveUserGuestOrCreate(principal, httpRequest, httpResponse)
        return userMono.flatMap { user ->
            cartService.getCartForUserOrGuest(user)
                .flatMap { cart ->
                    cartService.getCartItemsForCart(cart.cartId!!).collectList()
                        .map { cartItems ->
                            ResponseEntity.ok(mapOf("items" to cartItems))
                        }
                }
        }

    }

    @GetMapping("/cart-item-count")
    fun getCartItemCount(
        principal: Principal?,
        httpRequest: ServerHttpRequest,
    ): Mono<ResponseEntity<Map<String, Int>>> {
        return guestService.retrieveUserOrGuest(principal, httpRequest)
            .flatMap { user ->
                cartService.getCartItemCount(user)
                    .map {
                        ResponseEntity.ok(mapOf("cartItemCount" to it))
                    }
            }
            .defaultIfEmpty(ResponseEntity.ok(mapOf("cartItemCount" to 0)))

    }

}
