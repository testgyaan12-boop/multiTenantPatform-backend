package com.restaurant.saas.notification.service;

import com.restaurant.saas.notification.entity.Notification;
import com.restaurant.saas.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public NotificationService(
        NotificationRepository notificationRepository,
        JavaMailSender mailSender,
        org.springframework.core.env.Environment environment) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.fromAddress = environment.getProperty("spring.mail.username", "no-reply@resto-saas.local");
    }

    @Transactional
    public Notification create(Long tenantId, Long userId, String channel, String templateCode,
                               String subject, String body) {
        Notification notification = new Notification();
        notification.setTenantId(tenantId);
        notification.setUserId(userId);
        notification.setChannel(channel);
        notification.setTemplateCode(templateCode);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setStatus("PENDING");
        return notificationRepository.save(notification);
    }

    @Async
    public void sendEmail(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setSubject(notification.getSubject());
            message.setText(notification.getBody());
            mailSender.send(message);
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
        } catch (Exception ex) {
            log.warn("Failed to send notification {}: {}", notification.getId(), ex.getMessage());
            notification.setStatus("FAILED");
        }
        notificationRepository.save(notification);
    }
}
