package com.banking.notificationservice.controller;

import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        log.info("REST request to get notification by ID: {}", id);
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByAccount(@PathVariable String accountNumber) {
        log.info("REST request to get notifications for account: {}", accountNumber);
        List<NotificationResponse> notifications = notificationService.getNotificationsByAccount(accountNumber);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByTransaction(@PathVariable String transactionId) {
        log.info("REST request to get notifications for transaction: {}", transactionId);
        List<NotificationResponse> notifications = notificationService.getNotificationsByTransaction(transactionId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        log.info("REST request to get all notifications");
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is UP!");
    }
}