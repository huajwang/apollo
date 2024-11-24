package com.goodfeel.nightgrass.service

import com.stripe.Stripe
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class StripeService(@Value("\${stripe.secret-key}") secretKey: String) {
    init {
        Stripe.apiKey = secretKey
    }

    fun createCheckoutSession(
        amount: BigDecimal,
        currency: String,
        successUrl: String,
        cancelUrl: String
    ): Mono<Session> {
        return Mono.fromCallable {
            val params =
                SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(
                                        amount.multiply(BigDecimal.valueOf(100)).longValueExact()
                                    ) // amount in cents
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("E-commerce Product")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            Session.create(params)
        }
    }
}
