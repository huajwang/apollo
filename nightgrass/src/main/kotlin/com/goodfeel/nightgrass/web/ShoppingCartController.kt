package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.dto.CartItemDto
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.web.util.CartItemUpdateRequest
import com.goodfeel.nightgrass.web.util.AddCartRequest
import com.goodfeel.nightgrass.web.util.RemoveCartRequest
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Controller
@RequestMapping("/cart")
class ShoppingCartController(private val cartService: CartService) {
    private val logger: Logger = LoggerFactory.getLogger(ShoppingCartController::class.java)

    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    @GetMapping
    fun viewCart(model: Model): Mono<String> {
        val cartItemDtosFlux = cartService.getCartItems()
            .map {
                it.processProperties()
                it
            }
        val totalPriceMono = cartService.getTotalPrice()

        return Mono.zip(cartItemDtosFlux.collectList(), totalPriceMono)
            .map { tuple ->
                val cartItems = tuple.t1
                val cartTotal = tuple.t2
                model.addAttribute("cartItems", cartItems)
                model.addAttribute("cartTotal", cartTotal)
                "shopping-cart"
            }
    }

    @PostMapping("/add")
    fun addToCart(@RequestBody addCartRequest: AddCartRequest): Mono<ResponseEntity<Void>> {
        logger.debug("Adding product ${addCartRequest.productId} with properties: ${addCartRequest.properties}")
        return cartService.addProductToCart(addCartRequest)
            .then(Mono.just(ResponseEntity.ok().build()))
    }

    @PostMapping("/remove")
    fun removeFromCart(@RequestBody removeCartRequest: RemoveCartRequest): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Remove cartItem: ${removeCartRequest.itemId}")
        return cartService.removeCartItemFromCart(removeCartRequest.itemId)
            .flatMap {
                cartService.getCartItems()
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
    fun updateQuantity(@RequestBody updateRequest: CartItemUpdateRequest): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Received updateRequest request body from front end ajax call: {}", updateRequest)
        return cartService.updateQuantity(updateRequest.itemId, updateRequest.quantity!!)
            .flatMap {
                cartService.getCartItems().collectList()
                    .map { cartItems ->
                        ResponseEntity.ok(mapOf<String, Any>(
                            "message" to "Quantity updated",
                            "items" to cartItems)
                        )
                    }
            }
    }

    @PostMapping("/update-total-on-checkbox")
    fun updateTotal(@RequestBody request: CartItemUpdateRequest): Mono<ResponseEntity<Map<String, Any>>> {
        logger.debug("Received updateTotal request due to checkout update: {}", request)
        return cartService.updateCartTotalAndSelected(request.itemId, request.isChecked!!)
            .then(cartService.getCartItems().collectList())
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
    fun checkout(): Mono<ResponseEntity<Map<String, Long?>>> {
        return Utility.currentUserId.flatMap { userId: String ->
            cartService.createOrderAndCleanupShoppingCart(userId)
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

    @get:GetMapping("/total")
    val cartTotal: Mono<ResponseEntity<BigDecimal>>
        get() = cartService.getTotalPrice()
            .map<ResponseEntity<BigDecimal>> { body: BigDecimal? -> ResponseEntity.ok(body) }
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(BigDecimal.ZERO))


    @GetMapping("/items")
    fun getCartItems(): Mono<ResponseEntity<Map<String, Any>>> {
        return cartService.getCartItems().collectList()
            .map { cartItems ->
                ResponseEntity.ok(mapOf("items" to cartItems))
            }
    }

}
