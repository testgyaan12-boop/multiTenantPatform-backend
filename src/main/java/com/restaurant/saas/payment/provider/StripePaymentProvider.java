package com.restaurant.saas.payment.provider;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payments", name = "provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private final PaymentProperties properties;

    public StripePaymentProvider(PaymentProperties properties) {
        this.properties = properties;
        if (properties.stripe().apiKey() == null || properties.stripe().apiKey().isBlank()) {
            throw new IllegalStateException("STRIPE_API_KEY is required when PAYMENT_PROVIDER=stripe");
        }
        Stripe.apiKey = properties.stripe().apiKey();
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public PaymentResult charge(ChargeCommand command) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(command.amount().movePointRight(2).longValueExact())
                .setCurrency(command.currency().toLowerCase())
                .setDescription(command.description())
                .setReceiptEmail(command.customerEmail())
                .putMetadata("orderId", command.orderId().toString())
                .putMetadata("idempotencyKey", command.idempotencyKey())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true).build())
                .build();
            PaymentIntent intent = PaymentIntent.create(params, requestOptions(command.idempotencyKey()));
            return new PaymentResult(
                true, intent.getId(), intent.getStatus().toUpperCase(), null, intent.toJson());
        } catch (StripeException ex) {
            return new PaymentResult(false, null, "FAILED", ex.getMessage(), null);
        }
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(command.providerPaymentId())
                .setAmount(command.amount().movePointRight(2).longValueExact())
                .build();
            Refund refund = Refund.create(params);
            return new RefundResult(true, refund.getId(), refund.getStatus().toUpperCase(), refund.toJson());
        } catch (StripeException ex) {
            return new RefundResult(false, null, "FAILED", ex.getMessage());
        }
    }

    @Override
    public WebhookResult handleWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(
                payload, signature, properties.stripe().webhookSecret());
            return new WebhookResult(true, event.getId(), event.getType(), payload);
        } catch (SignatureVerificationException ex) {
            return new WebhookResult(false, null, null, payload);
        }
    }

    private static com.stripe.net.RequestOptions requestOptions(String idempotencyKey) {
        return com.stripe.net.RequestOptions.builder()
            .setIdempotencyKey(idempotencyKey)
            .build();
    }
}
