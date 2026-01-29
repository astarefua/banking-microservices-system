package com.banking.notificationservice.dto;

import com.banking.notificationservice.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String notificationId;
    private String recipient;
    private Notification.NotificationType type;
    private Notification.NotificationChannel channel;
    private String subject;
    private String message;
    private Notification.NotificationStatus status;
    private String transactionId;
    private String accountNumber;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}