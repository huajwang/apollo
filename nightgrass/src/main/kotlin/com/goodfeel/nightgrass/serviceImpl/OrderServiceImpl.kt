package com.goodfeel.nightgrass.serviceImpl

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.data.OrderItem
import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.dto.PlaceOrderRequest
import com.goodfeel.nightgrass.repo.OrderItemRepository
import com.goodfeel.nightgrass.repo.OrderRepository
import com.goodfeel.nightgrass.repo.UserRepository
import com.goodfeel.nightgrass.util.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class OrderServiceImpl(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)

    /**
     * Create an order for an authenticated user.
     * Links the order to the user's account.
     */
    fun createOrder(userId: String, request: PlaceOrderRequest): Mono<Order> {
        return Mono.defer {
            val orderNo = generateOrderNumber()
            val order = Order(
                orderId = null,
                orderNo = orderNo,
                userId = userId,
                deliveryAddress = "${request.address}, ${request.city}, ${request.postalCode}",
                contactName = request.fullName,
                contactPhone = request.phone,
                originalTotal = BigDecimal.valueOf(request.subtotal),
                discountedTotal = BigDecimal.valueOf(request.subtotal),
                hst = BigDecimal.valueOf(request.tax),
                shippingFee = BigDecimal.valueOf(request.shippingFee),
                finalTotal = BigDecimal.valueOf(request.total),
                createdAt = LocalDateTime.now(),
                orderStatus = OrderStatus.PENDING,
                payType = request.paymentMethod
            )

            logger.info("Creating order $orderNo for user $userId with total ${request.total}")

            orderRepository.save(order)
                .flatMap { savedOrder ->
                    // Save order items
                    val orderItems = request.items.map { item ->
                        OrderItem(
                            orderItemId = null,
                            orderId = savedOrder.orderId!!,
                            productName = item.productName,
                            imageUrl = item.imageUrl,
                            quantity = item.quantity,
                            properties = item.properties,
                            unitPrice = BigDecimal.valueOf(item.price)
                        )
                    }

                    Flux.fromIterable(orderItems)
                        .flatMap { orderItemRepository.save(it) }
                        .then(Mono.just(savedOrder))
                }
                .doOnSuccess {
                    logger.info("Order created successfully: ${it.orderNo}")
                }
                .doOnError {
                    logger.error("Failed to create order: ${it.message}")
                }
        }
    }

    /**
     * Create an order for a guest user.
     * Creates a guest user record and links the order to it.
     */
    fun createGuestOrder(request: PlaceOrderRequest): Mono<Order> {
        return Mono.defer {
            // Generate guest user ID
            val guestId = "guest_${System.currentTimeMillis()}_${(0..9999).random()}"
            
            // Create guest user
            val guestUser = User(
                id = null,
                oauthId = null,
                guestId = guestId,
                nickName = request.fullName,
                email = request.email,
                avatar = null,
                provider = null
            )

            logger.info("Creating guest user with ID: $guestId")

            userRepository.save(guestUser)
                .flatMap { _ ->
                    // Create order for guest
                    val orderNo = generateOrderNumber()
                    val order = Order(
                        orderId = null,
                        orderNo = orderNo,
                        userId = guestId,  // Use guest ID as userId
                        deliveryAddress = "${request.address}, ${request.city}, ${request.postalCode}",
                        contactName = request.fullName,
                        contactPhone = request.phone,
                        originalTotal = BigDecimal.valueOf(request.subtotal),
                        discountedTotal = BigDecimal.valueOf(request.subtotal),
                        hst = BigDecimal.valueOf(request.tax),
                        shippingFee = BigDecimal.valueOf(request.shippingFee),
                        finalTotal = BigDecimal.valueOf(request.total),
                        createdAt = LocalDateTime.now(),
                        orderStatus = OrderStatus.PENDING,
                        payType = request.paymentMethod
                    )

                    logger.info("Creating order $orderNo for guest $guestId with total ${request.total}")

                    orderRepository.save(order)
                        .flatMap { savedOrder ->
                            // Save order items
                            val orderItems = request.items.map { item ->
                                OrderItem(
                                    orderItemId = null,
                                    orderId = savedOrder.orderId!!,
                                    productName = item.productName,
                                    imageUrl = item.imageUrl,
                                    quantity = item.quantity,
                                    properties = item.properties,
                                    unitPrice = BigDecimal.valueOf(item.price)
                                )
                            }

                            Flux.fromIterable(orderItems)
                                .flatMap { orderItemRepository.save(it) }
                                .then(Mono.just(savedOrder))
                        }
                }
                .doOnSuccess {
                    logger.info("Guest order created successfully: ${it.orderNo}")
                }
                .doOnError {
                    logger.error("Failed to create guest order: ${it.message}")
                }
        }
    }

    /**
     * Generate a unique order number.
     * Format: ORD-<timestamp>-<random>
     */
    private fun generateOrderNumber(): String {
        return "ORD-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    fun getOrdersByUserId(userId: String): Flux<com.goodfeel.nightgrass.dto.OrderDto> {
        return orderRepository.findByUserId(userId)
            .flatMap { order ->
                orderItemRepository.findByOrderId(order.orderId!!)
                    .map { it.toDto() }
                    .collectList()
                    .map { items ->
                        order.toDto(items)
                    }
            }
    }
}

