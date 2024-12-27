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
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/pay")
class PaymentApiController(private val orderService: OrderService, private val stripeService: StripeService) {
    private val logger: Logger = LoggerFactory.getLogger(PaymentApiController::class.java)

    @PostMapping("/create-checkout-session")
    fun createCheckoutSession(@ModelAttribute checkoutRequest: CheckoutRequest): Mono<String> {
        val successUrl = "${Constant.SERVER_BASE_URL}/pay/success?orderId=${checkoutRequest.orderId}"
        val cancelUrl = "${Constant.SERVER_BASE_URL}/pay/cancel?orderId=${checkoutRequest.orderId}"

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
