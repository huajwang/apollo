package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.data.Order
import com.goodfeel.nightgrass.data.OrderItem
import com.goodfeel.nightgrass.service.OrderService
import com.goodfeel.nightgrass.serviceImpl.GuestService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import com.goodfeel.nightgrass.util.OrderStatus
import com.goodfeel.nightgrass.web.util.AddCartRequest
import com.goodfeel.nightgrass.web.util.Utility
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.Principal
import java.time.LocalDateTime

@Controller
@RequestMapping("/buynow")
class BuyNowController(
    private val orderService: OrderService,
    private val guestService: GuestService,
    private val productService: ProductService
    ) {

    @Value("\${STRIPE_PUBLIC_KEY}")
    private val stripePublicKey: String? = null

    @PostMapping
    fun buyNow(
        @ModelAttribute addCartRequest: AddCartRequest,
        principal: Principal?,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        model: Model
    ): Mono<String> {

        val userMono = guestService.retrieveUserGuestOrCreate(principal, request, response)
        val productDtoMono = productService.getProductById(addCartRequest.productId)
        return userMono.zipWith(productDtoMono) { user, product ->
            val productTotal =
                if (product.discountedPrice != null) product.discountedPrice else product.price

            val estimatedHST = productTotal?.multiply(
                BigDecimal.valueOf(Utility.HST))?.setScale(2, RoundingMode.HALF_UP)
                ?: throw IllegalStateException("product total is null")

            val orderTotalFinal = productTotal.add(estimatedHST)
                .setScale(2, RoundingMode.HALF_UP)

            val order = Order(
                orderNo = Utility.generateOrderNo(),
                userId = (user.oauthId) ?: (user.guestId)
                ?: throw IllegalArgumentException("Both userId and guestId is null"),
                createdAt = LocalDateTime.now(),
                orderStatus = OrderStatus.PENDING,
                originalTotal = product.price,
                discountedTotal =  product.discountedPrice ?: product.price,
                hst = estimatedHST,
                shippingFee = BigDecimal.ZERO, // TODO
                finalTotal = orderTotalFinal,
                // Copy user details to order
                contactName = user.customerName,
                contactPhone = user.phone,
                deliveryAddress = user.address
            )

            model.addAttribute("user", user)
            Pair(order, product)
        }.flatMap { pair ->
            orderService.updateOrder(pair.first)
                .doOnNext { order ->
                    model.addAttribute("order", order.toDto())
                }
                .flatMap { savedOrder ->
                    val orderItem = OrderItem(
                        orderId = savedOrder.orderId!!,
                        productName = pair.second.productName,
                        imageUrl = pair.second.imageUrl,
                        quantity = 1,
                        unitPrice = pair.second.price
                    )
                    addCartRequest.properties?.let {
                        orderItem.setPropertiesFromMap(it)
                    }
                    model.addAttribute("orderItems", listOf(orderItem.toDto()))
                    model.addAttribute("shippingDetails", "Delivery to garage")
                    model.addAttribute("STRIPE_PUBLIC_KEY", stripePublicKey)
                    orderService.save(orderItem)
                }
        }.thenReturn("checkout")
    }
}
