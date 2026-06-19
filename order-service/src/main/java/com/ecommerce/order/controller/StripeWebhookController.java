package com.ecommerce.order.controller;

import com.ecommerce.order.service.OrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Receives asynchronous payment confirmations from Stripe.
 *
 * IMPORTANT: this is why we don't confirm orders synchronously when creating them.
 * The frontend collects card details via Stripe.js, Stripe processes the charge,
 * and ONLY THEN does Stripe call this webhook to tell us payment succeeded.
 * This can take seconds (3D Secure, bank delays) — orders sit in PENDING until then.
 */
@RestController
@RequestMapping("/api/v1/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Stripe payment webhooks")
public class StripeWebhookController {

    private final OrderService orderService;

    @Value("${app.stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    @Operation(summary = "Stripe webhook endpoint — verifies signature before processing")
    public ResponseEntity<String> handleWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event;
        try {
            // CRITICAL: always verify the signature. Without this, anyone could
            // POST a fake "payment succeeded" event and get free orders confirmed.
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature received");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
                orderService.confirmOrder(intent.getId());
                log.info("Processed payment_intent.succeeded for {}", intent.getId());
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
                log.warn("Payment failed for intent {}", intent.getId());
                // Could trigger a "payment failed" notification here
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("OK");
    }
}
