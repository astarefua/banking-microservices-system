package com.banking.notificationservice.service;

import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.event.TransactionCompletedEvent;
import com.banking.notificationservice.event.TransactionCreatedEvent;
import com.banking.notificationservice.model.Notification;
import com.banking.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("Handling TransactionCreatedEvent: {}", event.getTransactionId());

        // Create notification
        String subject = "Transaction Initiated";
        String message = String.format(
                "Your %s transaction of %s %s has been initiated. Transaction ID: %s",
                event.getType(),
                event.getCurrency(),
                event.getAmount(),
                event.getTransactionId()
        );

        Notification notification = createNotification(
                event.getFromAccount() + "@example.com", // In real app, get email from Account Service
                Notification.NotificationType.TRANSACTION_CREATED,
                subject,
                message,
                event.getTransactionId(),
                event.getFromAccount()
        );

        // Send notification
        sendNotification(notification);
    }

    @Transactional
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Handling TransactionCompletedEvent: {}", event.getTransactionId());

        String subject = "COMPLETED".equals(event.getStatus())
                ? "Transaction Successful"
                : "Transaction Failed";

        String message = String.format(
                "Your %s transaction of %s has been %s. Transaction ID: %s",
                event.getType(),
                event.getAmount(),
                event.getStatus().toLowerCase(),
                event.getTransactionId()
        );

        Notification notification = createNotification(
                event.getFromAccount() + "@example.com",
                Notification.NotificationType.TRANSACTION_COMPLETED,
                subject,
                message,
                event.getTransactionId(),
                event.getFromAccount()
        );

        // Send notification
        sendNotification(notification);

        // If it's a transfer, also notify the recipient
        if (event.getToAccount() != null && !event.getToAccount().isEmpty()) {
            String recipientMessage = String.format(
                    "You have received a transfer of %s. Transaction ID: %s",
                    event.getAmount(),
                    event.getTransactionId()
            );

            Notification recipientNotification = createNotification(
                    event.getToAccount() + "@example.com",
                    Notification.NotificationType.TRANSACTION_COMPLETED,
                    "Money Received",
                    recipientMessage,
                    event.getTransactionId(),
                    event.getToAccount()
            );

            sendNotification(recipientNotification);
        }
    }

    private Notification createNotification(
            String recipient,
            Notification.NotificationType type,
            String subject,
            String message,
            String transactionId,
            String accountNumber) {

        String notificationId = UUID.randomUUID().toString();

        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .recipient(recipient)
                .type(type)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject(subject)
                .message(message)
                .status(Notification.NotificationStatus.PENDING)
                .transactionId(transactionId)
                .accountNumber(accountNumber)
                .build();

        return notificationRepository.save(notification);
    }

    private void sendNotification(Notification notification) {
        try {
            log.info("Sending notification: {}", notification.getNotificationId());

            // Send based on channel
            switch (notification.getChannel()) {
                case EMAIL -> emailService.sendEmail(
                        notification.getRecipient(),
                        notification.getSubject(),
                        notification.getMessage()
                );
                case SMS -> emailService.sendSMS(
                        notification.getRecipient(),
                        notification.getMessage()
                );
                case PUSH -> log.info("Push notification not implemented yet");
            }

            // Update notification status
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Notification sent successfully: {}", notification.getNotificationId());

        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getNotificationId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    public NotificationResponse getNotificationById(Long id) {
        log.info("Fetching notification by ID: {}", id);
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));
        return mapToResponse(notification);
    }

    public List<NotificationResponse> getNotificationsByAccount(String accountNumber) {
        log.info("Fetching notifications for account: {}", accountNumber);
        return notificationRepository.findByAccountNumber(accountNumber).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getNotificationsByTransaction(String transactionId) {
        log.info("Fetching notifications for transaction: {}", transactionId);
        return notificationRepository.findByTransactionId(transactionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<NotificationResponse> getAllNotifications() {
        log.info("Fetching all notifications");
        return notificationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationId(notification.getNotificationId())
                .recipient(notification.getRecipient())
                .type(notification.getType())
                .channel(notification.getChannel())
                .subject(notification.getSubject())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .transactionId(notification.getTransactionId())
                .accountNumber(notification.getAccountNumber())
                .errorMessage(notification.getErrorMessage())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .build();
    }
}