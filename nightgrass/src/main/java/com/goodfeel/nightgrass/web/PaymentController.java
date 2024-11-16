package com.goodfeel.nightgrass.web;

import com.goodfeel.nightgrass.service.StripeService;
import com.goodfeel.nightgrass.web.util.CheckoutRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Value("${STRIPE_PUBLIC_KEY}")
    private String stripePublicKey;

    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/create-checkout-session")
    public Mono<String> createCheckoutSession(@ModelAttribute CheckoutRequest checkoutRequest) {
        String successUrl = "http://localhost:8080/success";
        String cancelUrl = "http://localhost:8080/cancel";

        logger.debug("The amount = {}", checkoutRequest.amount);
        return stripeService.createCheckoutSession(checkoutRequest.amount, "cad", successUrl, cancelUrl)
                .map(Session::getUrl);
    }
}
