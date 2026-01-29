package com.banking.notificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String notificationId;

    @Column(nullable = false, length = 100)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(length = 50)
    private String transactionId;

    @Column(length = 20)
    private String accountNumber;

    @Column(length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime sentAt;

    public enum NotificationType {
        TRANSACTION_CREATED,
        TRANSACTION_COMPLETED,
        TRANSACTION_FAILED,
        ACCOUNT_CREATED,
        BALANCE_LOW,
        FRAUD_ALERT
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }
}