package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.service.StripeService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.CheckoutRequest
import com.stripe.model.checkout.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PaymentController(private val orderService: OrderService, private val stripeService: StripeService) {
    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    @PostMapping("/create-checkout-session")
    fun createCheckoutSession(@ModelAttribute checkoutRequest: CheckoutRequest): Mono<String> {
        val successUrl = "http://localhost:8080/success"
        val cancelUrl = "http://localhost:8080/cancel"

        logger.debug("The amount = {}", checkoutRequest.amount)
        return orderService.findOrderById(checkoutRequest.orderId)
            .flatMap { order: Order ->
                order.finalTotal = checkoutRequest.amount
                order.orderStatus = OrderStatus.SUBMITTED
                orderService.updateOrder(order)
            }
            .then(stripeService.createCheckoutSession(checkoutRequest.amount, "cad", successUrl, cancelUrl))
            .map { obj: Session -> obj.url }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(PaymentController::class.java)
    }
}
