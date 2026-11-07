package com.restaurant.saas.payment.web;

import com.restaurant.saas.payment.entity.Payment;
import com.restaurant.saas.payment.entity.PaymentWebhookEvent;
import com.restaurant.saas.payment.entity.Refund;
import com.restaurant.saas.payment.service.PaymentService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public record PayRequest(@NotBlank String method) {
    }

    public record RefundRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String reason
    ) {
    }

    @PostMapping("/public/orders/{publicToken}/pay")
    public Payment pay(
        @PathVariable String publicToken,
        @RequestHeader("Idempotency-Key") String idempotencyKey,
        @RequestBody(required = false) PayRequest request) {
        return paymentService.payOrder(
            publicToken, idempotencyKey, request == null ? "card" : request.method());
    }

    @PostMapping("/payments/{paymentId}/refunds")
    @PreAuthorize("hasAuthority('payment.refund')")
    public ResponseEntity<Refund> refund(
        @PathVariable Long paymentId,
        @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.refund(paymentId, request.amount(), request.reason()));
    }

    @PostMapping("/payments/webhooks/{provider}")
    public PaymentWebhookEvent webhook(
        @PathVariable String provider,
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return paymentService.processWebhook(payload, signature);
    }
}
