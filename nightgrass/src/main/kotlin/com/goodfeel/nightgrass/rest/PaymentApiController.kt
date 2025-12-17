package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.StripeService
import com.goodfeel.nightgrass.serviceImpl.ReferralTrackingService
import com.goodfeel.nightgrass.util.Constant
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.util.ReferralRewardStatus
import com.goodfeel.nightgrass.web.util.CheckoutRequest
import com.stripe.model.checkout.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.net.URI

@RestController
@RequestMapping("/pay")
class PaymentApiController(
    private val orderService: OrderService,
    private val stripeService: StripeService,
    private val referralTrackingService: ReferralTrackingService)
{
    private val logger: Logger = LoggerFactory.getLogger(PaymentApiController::class.java)

    @PostMapping("/create-checkout-session")
    fun createCheckoutSession(@ModelAttribute checkoutRequest: CheckoutRequest): Mono<String> {
        val successUrl = "${Constant.SERVER_BASE_URL}/pay/success?orderId=${checkoutRequest.orderId}"
        val cancelUrl = "${Constant.SERVER_BASE_URL}/pay/cancel?orderId=${checkoutRequest.orderId}"

        logger.debug("The amount = {}", checkoutRequest.amount)
        return orderService.findOrderById(checkoutRequest.orderId)
            .flatMap { order: Order ->
                order.contactName = checkoutRequest.contactName
                order.contactPhone = checkoutRequest.contactPhone
                order.deliveryAddress = checkoutRequest.deliveryAddress

                order.finalTotal = checkoutRequest.amount
                order.orderStatus = OrderStatus.PENDING
                orderService.updateOrder(order)
            }
            .then(stripeService.createCheckoutSession(checkoutRequest.amount, "cad", successUrl, cancelUrl))
            .map { obj: Session -> obj.url }

    }

    @GetMapping("/success")
    fun paymentSuccess(
        @RequestParam orderId: Long,
        exchange: ServerWebExchange
    ): Mono<Void> {
        return orderService.findOrderById(orderId)
            .flatMap { order ->
                if (order.orderStatus != OrderStatus.PAID) {
                    order.orderStatus = OrderStatus.PAID
                    orderService.updateOrder(order)
                        .flatMap { savedOrder ->
                            // Check for referral
                            exchange.session.flatMap { session ->
                                val sharerId = session.attributes["sharerId"] as? String
                                if (sharerId != null) {
                                    val rewardAmount = savedOrder.finalTotal.multiply(BigDecimal("0.02"))
                                    referralTrackingService.rewardSharer(
                                        sharerId,
                                        rewardAmount,
                                        savedOrder.orderId!!,
                                        ReferralRewardStatus.PENDING
                                    )
                                } else {
                                    Mono.empty()
                                }
                            }
                        }
                } else {
                    Mono.empty()
                }
            }
            .then(Mono.defer {
                exchange.response.statusCode = HttpStatus.FOUND
                exchange.response.headers.location = URI.create("/order-complete?orderId=$orderId")
                Mono.empty()
            })
    }
}
