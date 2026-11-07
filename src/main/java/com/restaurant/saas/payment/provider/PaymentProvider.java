package com.restaurant.saas.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {

    String name();

    PaymentResult charge(ChargeCommand command);

    RefundResult refund(RefundCommand command);

    WebhookResult handleWebhook(String payload, String signature);

    record ChargeCommand(
        String idempotencyKey,
        Long orderId,
        BigDecimal amount,
        String currency,
        String customerEmail,
        String description
    ) {
    }

    record PaymentResult(
        boolean success,
        String providerPaymentId,
        String status,
        String failureReason,
        String rawResponse
    ) {
    }

    record RefundCommand(String providerPaymentId, BigDecimal amount, String reason) {
    }

    record RefundResult(boolean success, String providerRefundId, String status, String rawResponse) {
    }

    record WebhookResult(boolean signatureValid, String eventId, String eventType, String payload) {
    }
}
