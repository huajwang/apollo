package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.StripeService
import com.goodfeel.nightgrass.util.Constant
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.CheckoutRequest
import com.stripe.model.checkout.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/pay")
class PaymentApiController(private val orderService: OrderService, private val stripeService: StripeService) {
    private val logger: Logger = LoggerFactory.getLogger(PaymentApiController::class.java)

    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    @PostMapping("/create-checkout-session")
    fun createCheckoutSession(@ModelAttribute checkoutRequest: CheckoutRequest): Mono<String> {
        val successUrl = "${Constant.baseUrl}/pay/success?orderId=${checkoutRequest.orderId}"
        val cancelUrl = "${Constant.baseUrl}/pay/cancel?orderId=${checkoutRequest.orderId}"

        logger.debug("The amount = {}", checkoutRequest.amount)
        return orderService.findOrderById(checkoutRequest.orderId)
            .flatMap { order: Order ->
                order.finalTotal = checkoutRequest.amount
                order.orderStatus = OrderStatus.PENDING
                orderService.updateOrder(order)
            }
            .then(stripeService.createCheckoutSession(checkoutRequest.amount, "cad", successUrl, cancelUrl))
            .map { obj: Session -> obj.url }

    }
}
