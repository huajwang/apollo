package com.goodfeel.nightgrass.rest;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class WebhookController {

    private final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${stripe.webhook.secret}") // Set this value in application.properties
    private String endpointSecret;

    @PostMapping("/webhook")
    public Mono<ResponseEntity<String>> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // Verify the signature
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature"));
        }

        // Handle the event type
        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (paymentIntent != null) {
                    // Handle successful payment here
                    logger.debug("Payment succeeded: {}", paymentIntent.getId());
                    // Update database, notify user, etc.
                }
                break;
            case "checkout.session.completed":
                // Handle checkout completion
                logger.debug("Checkout session completed");
                // Perform post-checkout actions
                break;
            // Add more cases as needed for other events
            default:
                logger.debug("Unhandled event type: {}", event.getType());
                break;
        }

        return Mono.just(ResponseEntity.ok("Received"));
    }
}
