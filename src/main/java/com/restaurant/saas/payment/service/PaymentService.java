package com.restaurant.saas.payment.service;

import com.restaurant.saas.common.exception.ConflictException;
import com.restaurant.saas.common.exception.NotFoundException;
import com.restaurant.saas.order.entity.Order;
import com.restaurant.saas.order.repository.OrderRepository;
import com.restaurant.saas.payment.entity.Payment;
import com.restaurant.saas.payment.entity.PaymentWebhookEvent;
import com.restaurant.saas.payment.entity.Refund;
import com.restaurant.saas.payment.provider.PaymentProvider;
import com.restaurant.saas.payment.repository.PaymentRepository;
import com.restaurant.saas.payment.repository.PaymentWebhookEventRepository;
import com.restaurant.saas.payment.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentProvider provider;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentWebhookEventRepository webhookRepository;
    private final OrderRepository orderRepository;

    public PaymentService(
        PaymentProvider provider,
        PaymentRepository paymentRepository,
        RefundRepository refundRepository,
        PaymentWebhookEventRepository webhookRepository,
        OrderRepository orderRepository) {
        this.provider = provider;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.webhookRepository = webhookRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Payment payOrder(String publicToken, String idempotencyKey, String method) {
        Order order = orderRepository.findByPublicToken(publicToken)
            .orElseThrow(() -> new NotFoundException("Order not found"));
        if ("PAID".equals(order.getPaymentStatus())) {
            return paymentRepository.findByOrderIdAndStatus(order.getId(), "PAID")
                .orElseThrow(() -> new ConflictException("already_paid", "Order is already paid"));
        }

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setTenantId(order.getTenantId());
        payment.setOrderId(order.getId());
        payment.setProvider(provider.name());
        payment.setStatus("PENDING");
        payment.setAmount(order.getGrandTotal());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentMethod(method);
        payment.setIdempotencyKey(idempotencyKey);
        payment = paymentRepository.save(payment);

        PaymentProvider.PaymentResult result = provider.charge(new PaymentProvider.ChargeCommand(
            idempotencyKey,
            order.getId(),
            order.getGrandTotal(),
            order.getCurrency(),
            order.getCustomerEmail(),
            "Order " + order.getOrderNumber()
        ));

        payment.setProviderPaymentId(result.providerPaymentId());
        payment.setRawResponse(result.rawResponse());
        if (result.success()) {
            payment.setStatus("PAID");
            payment.setCapturedAt(Instant.now());
            order.setPaymentStatus("PAID");
        } else {
            payment.setStatus("FAILED");
            payment.setFailureReason(result.failureReason());
            order.setPaymentStatus("FAILED");
        }
        paymentRepository.save(payment);
        orderRepository.save(order);
        return payment;
    }

    @Transactional
    public Refund refund(Long paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new NotFoundException("Payment", paymentId));
        if (!"PAID".equals(payment.getStatus())) {
            throw new ConflictException("not_refundable", "Only paid payments can be refunded");
        }
        BigDecimal refundable = payment.getAmount().subtract(payment.getRefundedAmount());
        if (amount.compareTo(refundable) > 0) {
            throw new ConflictException("refund_exceeds", "Refund amount exceeds the remaining balance");
        }

        Refund refund = new Refund();
        refund.setTenantId(payment.getTenantId());
        refund.setPaymentId(payment.getId());
        refund.setAmount(amount);
        refund.setReason(reason);
        refund.setStatus("PENDING");
        refund = refundRepository.save(refund);

        PaymentProvider.RefundResult result = provider.refund(
            new PaymentProvider.RefundCommand(payment.getProviderPaymentId(), amount, reason));

        refund.setProviderRefundId(result.providerRefundId());
        refund.setStatus(result.success() ? "SUCCEEDED" : "FAILED");
        refund.setProcessedAt(Instant.now());
        refundRepository.save(refund);

        if (result.success()) {
            payment.setRefundedAmount(payment.getRefundedAmount().add(amount));
            if (payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0) {
                payment.setStatus("REFUNDED");
            }
            paymentRepository.save(payment);
        }
        return refund;
    }

    @Transactional
    public PaymentWebhookEvent processWebhook(String payload, String signature) {
        PaymentProvider.WebhookResult result = provider.handleWebhook(payload, signature);
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProvider(provider.name());
        event.setEventId(result.eventId() == null ? "invalid-" + Instant.now().toEpochMilli() : result.eventId());
        event.setEventType(result.eventType() == null ? "unknown" : result.eventType());
        event.setPayload(payload);
        event.setSignatureValid(result.signatureValid());
        event.setProcessedAt(Instant.now());
        if (!result.signatureValid()) {
            event.setErrorMessage("Signature verification failed");
        }
        return webhookRepository.save(event);
    }
}
