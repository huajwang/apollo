package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.util.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/pay")
class PaymentController(private val orderService: OrderService) {

    private val logger = LoggerFactory.getLogger(PaymentController::class.java)

    @GetMapping("/success")
    fun paySuccess(@RequestParam orderId: Long): Mono<String> {
        return orderService.findOrderById(orderId).flatMap { order ->
            order.orderStatus = OrderStatus.PENDING
            orderService.updateOrder(order)
        }
            .doOnSuccess { logger.info("Order status updated to PROCESSING for order ID: $orderId") }
            .thenReturn("redirect:/pay/show-payment-success?orderId=$orderId")
    }

    @GetMapping("/cancel")
    fun payCancel(@RequestParam orderId: Long) : Mono<String> {
        logger.debug("Customer cancel the payment: $orderId")
        return Mono.just("redirect:/checkout?orderId=$orderId")
    }

    @GetMapping("/show-payment-success")
    fun showPaymentSuccessToUser(@RequestParam orderId: Long, model: Model): Mono<String> {
        return orderService.findOrderById(orderId)
            .flatMap {
                model.addAttribute("order", it)
                Mono.just("payment-success")
            }
            .switchIfEmpty(
                Mono.fromCallable {
                    model.addAttribute("error", "Order with ID $orderId is not found!")
                    "error-page"
                }
            )
            .onErrorResume { ex ->
                model.addAttribute("error", "Unable to load order details: ${ex.message}")
                Mono.just("error-page")
            }
    }

}
