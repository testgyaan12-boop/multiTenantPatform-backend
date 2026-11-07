package com.restaurant.saas.payment.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.payments", name = "provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(SandboxPaymentProvider.class);

    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public PaymentResult charge(ChargeCommand command) {
        String paymentId = "sbx_" + UUID.randomUUID();
        log.info("Sandbox charge idempotencyKey={} order={} amount={} {}",
            command.idempotencyKey(), command.orderId(), command.amount(), command.currency());
        return new PaymentResult(
            true,
            paymentId,
            "PAID",
            null,
            "{\"provider\":\"sandbox\",\"paymentId\":\"" + paymentId + "\"}"
        );
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        String refundId = "sbx_rf_" + UUID.randomUUID();
        return new RefundResult(
            true,
            refundId,
            "SUCCEEDED",
            "{\"provider\":\"sandbox\",\"refundId\":\"" + refundId + "\"}"
        );
    }

    @Override
    public WebhookResult handleWebhook(String payload, String signature) {
        return new WebhookResult(true, UUID.randomUUID().toString(), "sandbox.event", payload);
    }
}
