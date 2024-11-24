package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.serviceImpl.CartService
import com.goodfeel.nightgrass.web.util.CartItemUpdateRequest
import com.goodfeel.nightgrass.web.util.AddCartRequest
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
class CartController(private val cartService: CartService) {
    private val logger: Logger = LoggerFactory.getLogger(CartController::class.java)

    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    // View cart contents
    @GetMapping
    fun viewCart(model: Model): Mono<String> {
        val cartItemsFlux = cartService.getCartItems()
        val totalPriceMono = cartService.getTotalPrice()

        return Mono.zip(cartItemsFlux.collectList(), totalPriceMono)
            .map { tuple ->
                val cartItems = tuple.t1
                val cartTotal = tuple.t2
                model.addAttribute("cartItems", cartItems)
                model.addAttribute("cartTotal", cartTotal)
                "shopping-cart"
            }
    }

    @PostMapping("/add")
    fun addToCart(@ModelAttribute addCartRequest: AddCartRequest): Mono<String> {
        logger.debug("Adding product ${addCartRequest.productId} with properties: ${addCartRequest.itemId}")
        return cartService.addProductToCart(addCartRequest)
            .thenReturn("redirect:/product/detail?id=${addCartRequest.productId}")
    }

    @PostMapping("/remove")
    fun removeFromCart(@ModelAttribute addCartRequest: AddCartRequest, model: Model): Mono<String> {
        return addCartRequest.itemId?.let { itemId ->
            cartService.removeCartItemFromCart(itemId)
                .doOnSuccess {
                    model.addAttribute("message", "Item removed from the cart successfully!")
                    logger.debug("Cart item $itemId removed successfully")
                }
                .onErrorResume { error ->
                    logger.error("Error removing item $itemId: ${error.message}")
                    model.addAttribute("errorMessage",
                        "Failed to remove the item from the cart.")
                    Mono.empty()
                }
                .thenReturn("redirect:/cart")
        } ?: run {
            logger.error("Invalid itemId in request") // Logging null case
            Mono.just("redirect:/error")
        }
    }

    @PostMapping("/update-quantity")
    fun updateQuantity(@RequestBody updateRequest: CartItemUpdateRequest): Mono<ResponseEntity<String>> {
        logger.debug("Received updateRequest request body from front end ajax call: {}", updateRequest)
        return cartService.updateQuantity(updateRequest.itemId, updateRequest.quantity!!) // TODO
            .map { ResponseEntity.ok("Quantity updated") }
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found"))
            .onErrorResume { ex ->
                logger.error("Error occurred while updating quantity", ex)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update quantity"))
            }
    }

    @PostMapping("/update-total-on-checkbox")
    fun updateTotal(@RequestBody request: CartItemUpdateRequest): Mono<ResponseEntity<Map<String, String>>> {
        val response: MutableMap<String, String> = HashMap()
        response["message"] = "Cart total updated on checkbox change"
        return cartService.updateCartTotal(request.itemId, request.isChecked!!) // TODO
            .thenReturn(ResponseEntity.ok(response))
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
}
