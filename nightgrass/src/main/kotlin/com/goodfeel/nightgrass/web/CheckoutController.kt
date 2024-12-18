package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.data.User
import com.goodfeel.nightgrass.dto.CustomerAndOrderInfoDto
import com.goodfeel.nightgrass.dto.OrderDto
import com.goodfeel.nightgrass.dto.OrderItemDto
import com.goodfeel.nightgrass.dto.UserDto
import com.goodfeel.nightgrass.service.IOrderService
import com.goodfeel.nightgrass.service.UserService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.serviceImpl.ReferralTrackingService
import com.goodfeel.nightgrass.util.Constant
import com.goodfeel.nightgrass.util.ReferralRewardStatus
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import reactor.util.function.Tuple3
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.Principal

@Controller
class CheckoutController(
    private val orderService: IOrderService,
    private val userService: UserService,
    private val referralTrackingService: ReferralTrackingService,
    private val guestService: GuestService
) {
    private val logger: Logger = LoggerFactory.getLogger(CheckoutController::class.java)

    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    @GetMapping("/checkout")
    fun processOrder(
        @RequestParam orderId: Long,
        model: Model, exchange: ServerWebExchange,
        principal: Principal?,
        httpRequest: ServerHttpRequest,
        httpResponse: ServerHttpResponse
    ): Mono<String> {
        logger.debug("The orderId = {}", orderId)
        val userMono = guestService.retrieveUserGuestOrCreate(principal, httpRequest, httpResponse)

        val orderItemDtosMono = orderService.getOrderItemsByOrderId(orderId)
            .map {
                it.processProperties()
                it
            }
            .collectList()
        val orderMono = orderService.getOrderById(orderId)

        return Mono.zip<User, List<OrderItemDto>, OrderDto>(userMono, orderItemDtosMono, orderMono)
            .flatMap { tuple: Tuple3<User, List<OrderItemDto>, OrderDto> ->
                val user = tuple.t1
                val orderItems = tuple.t2
                val order = tuple.t3

                val originalTotal: BigDecimal = order.orderTotal.setScale(2, RoundingMode.HALF_UP)

                // Apply 10% discount
                val discount = originalTotal.multiply(BigDecimal.valueOf(0.10))
                    .setScale(2, RoundingMode.HALF_UP)
                val discountedTotal = originalTotal.subtract(discount)
                    .setScale(2, RoundingMode.HALF_UP)

                // Apply 13% HST on the discounted total
                val estimatedHST = discountedTotal.multiply(BigDecimal.valueOf(Utility.HST))
                    .setScale(2, RoundingMode.HALF_UP)

                // Calculate final order total
                val orderTotalFinal = discountedTotal.add(estimatedHST)
                    .setScale(2, RoundingMode.HALF_UP)

                model.addAttribute("user", user)
                model.addAttribute("orderItems", orderItems)
                model.addAttribute("order", order)
                model.addAttribute("discount", discount)
                model.addAttribute("discountedTotal", discountedTotal)
                model.addAttribute("shippingDetails", "Delivery to garage")
                model.addAttribute("estimatedHST", estimatedHST)
                model.addAttribute("orderTotalFinal", orderTotalFinal)
                model.addAttribute("STRIPE_PUBLIC_KEY", stripePublicKey)
                exchange.session
                    .flatMap { session: WebSession ->
                        val sharerId = session.getAttribute<String>("sharerId")
                        if (sharerId != null) {
                            val reward = originalTotal.multiply(BigDecimal.valueOf(0.10))
                                .setScale(2, RoundingMode.HALF_UP)
                            // Persist or process the reward
                            return@flatMap referralTrackingService
                                .rewardSharer(
                                    sharerId, reward, order.orderId,
                                    ReferralRewardStatus.PENDING
                                )
                        }
                        Mono.empty()
                    }.thenReturn("checkout")
            }
    }

    @PostMapping("/update-user-info")
    fun updateCustomerInfo(
        @RequestBody customerInfoDto: CustomerAndOrderInfoDto,
        principal: Principal?,
        httpRequest: ServerHttpRequest,
        httpResponse: ServerHttpResponse
    ): Mono<ResponseEntity<UserDto>> {
        logger.debug("Received CustomerAndOrderInfoDto from front end: {}", customerInfoDto)
        val userMono = guestService.retrieveUserGuestOrCreate(principal, httpRequest, httpResponse)
        return userMono
            .flatMap { user ->
                // Update user properties with new data
                user.customerName = customerInfoDto.customerName
                user.phone = customerInfoDto.phone
                user.address = customerInfoDto.address

                userService.updateUserInfo(user)
            }

            .flatMap { updatedUser: User ->
                // Check if an orderId was provided in the request
                return@flatMap orderService.findOrderById(customerInfoDto.orderId)
                    .flatMap { order: Order ->
                        // Update order’s contact information
                        order.contactName = updatedUser.customerName
                        order.contactPhone = updatedUser.phone
                        order.deliveryAddress = updatedUser.address
                        orderService.updateOrder(order)
                    }
                    .then(Mono.just(updatedUser))
            }
            .map<ResponseEntity<UserDto>> { updatedUser: User ->
                // Convert the updated user to UserDto for response
                val updatedUserDto = UserDto(
                    updatedUser.customerName,
                    updatedUser.phone,
                    updatedUser.address
                )
                ResponseEntity.ok(updatedUserDto)
            }
            .defaultIfEmpty(ResponseEntity.notFound().build())
    }
}
