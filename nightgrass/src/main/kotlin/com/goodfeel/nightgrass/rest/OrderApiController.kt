package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.dto.PlaceOrderRequest
import com.goodfeel.nightgrass.dto.OrderDto
import com.goodfeel.nightgrass.serviceImpl.OrderServiceImpl
import com.goodfeel.nightgrass.util.AuthenticationUtility
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/orders")
class OrderApiController(
    private val orderService: OrderServiceImpl,
    private val authenticationUtility: AuthenticationUtility
) {

    private val logger = LoggerFactory.getLogger(OrderApiController::class.java)

    @GetMapping("/my-orders")
    fun getMyOrders(exchange: ServerWebExchange): Flux<OrderDto> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMapMany { user ->
                orderService.getOrdersByUserId(user.oauthId!!)
            }
            .onErrorResume { error ->
                logger.error("Failed to retrieve orders: {}", error.message)
                Flux.empty()
            }
    }

    @GetMapping("/{orderId}")
    fun getOrderById(@PathVariable orderId: Long): Mono<ResponseEntity<OrderDto>> {
        return orderService.getOrderById(orderId)
            .map { ResponseEntity.ok(it) }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }

    /**
     * Place an order from cart.
     * Works for both authenticated users and guests.
     * For authenticated users, the order is linked to their account.
     * For guests, a guest user record is created.
     */
    @PostMapping("/place")
    fun placeOrder(
        @RequestBody request: PlaceOrderRequest,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<OrderDto>> {
        return authenticationUtility.extractUserFromExchange(exchange)
            .flatMap { user ->
                // Authenticated user - use their OAuth ID
                orderService.createOrder(
                    userId = user.oauthId!!,
                    request = request
                )
                    .map { order -> ResponseEntity.ok(order.toDto()) }
            }
            .onErrorResume { error ->
                // If token extraction fails, treat as guest user
                logger.debug("No valid token found, processing as guest order: {}", error.message)
                orderService.createGuestOrder(request = request)
                    .map { order -> ResponseEntity.ok(order.toDto()) }
            }
            .doOnSuccess { logger.info("Order placed successfully") }
            .onErrorResume { error ->
                logger.error("Failed to place order: {}", error.message, error)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }
}
