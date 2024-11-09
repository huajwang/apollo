package com.goodfeel.nightgrass.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.PaymentMethodType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StripeService {

    public StripeService(@Value("${stripe.secret-key}") String secretKey) { Stripe.apiKey = secretKey; }

    public Mono<Session> createCheckoutSession(Double amount, String currency, String successUrl, String cancelUrl) {
        return Mono.fromCallable(() -> {
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency)
                                                    .setUnitAmount((long)(amount * 100)) // amount in cents
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("E-commerce Product")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            return Session.create(params);
        });
    }
}
