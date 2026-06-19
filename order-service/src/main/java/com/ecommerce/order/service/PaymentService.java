package com.ecommerce.order.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class PaymentService {

    @Value("${app.stripe.secret-key}")
    private String stripeSecretKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        com.stripe.Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Creates a Stripe PaymentIntent for the order total.
     * Returns the client secret — the frontend uses this with Stripe.js
     * to collect card details and confirm payment WITHOUT the card
     * number ever touching our backend (PCI compliance).
     */
    public PaymentIntentResult createPaymentIntent(BigDecimal amount, String currency, String orderNumber) {
        try {
            // Stripe expects amounts in the smallest currency unit (cents for EUR)
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .putMetadata("order_number", orderNumber)
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent intent = PaymentIntent.create(params);
            log.info("Created PaymentIntent {} for order {}", intent.getId(), orderNumber);

            return new PaymentIntentResult(intent.getId(), intent.getClientSecret());

        } catch (StripeException e) {
            log.error("Stripe error creating payment intent for order {}: {}", orderNumber, e.getMessage());
            throw new PaymentProcessingException("Failed to initialize payment: " + e.getMessage());
        }
    }

    public record PaymentIntentResult(String paymentIntentId, String clientSecret) {}

    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) {
            super(message);
        }
    }
}
