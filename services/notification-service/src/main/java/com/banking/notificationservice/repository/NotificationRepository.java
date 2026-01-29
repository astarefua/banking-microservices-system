package com.banking.notificationservice.repository;

import com.banking.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(String notificationId);

    List<Notification> findByRecipient(String recipient);

    List<Notification> findByAccountNumber(String accountNumber);

    List<Notification> findByTransactionId(String transactionId);

    List<Notification> findByStatus(Notification.NotificationStatus status);
}