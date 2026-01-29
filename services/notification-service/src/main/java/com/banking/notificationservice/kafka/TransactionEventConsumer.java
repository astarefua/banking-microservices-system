package com.banking.notificationservice.kafka;

import com.banking.notificationservice.event.TransactionCompletedEvent;
import com.banking.notificationservice.event.TransactionCreatedEvent;
import com.banking.notificationservice.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "transaction-created", groupId = "notification-service-group")
    public void consumeTransactionCreated(String message) {
        try {
            log.info("Received TransactionCreatedEvent: {}", message);
            TransactionCreatedEvent event = objectMapper.readValue(message, TransactionCreatedEvent.class);
            notificationService.handleTransactionCreated(event);
        } catch (Exception e) {
            log.error("Error processing TransactionCreatedEvent", e);
        }
    }

    @KafkaListener(topics = "transaction-completed", groupId = "notification-service-group")
    public void consumeTransactionCompleted(String message) {
        try {
            log.info("Received TransactionCompletedEvent: {}", message);
            TransactionCompletedEvent event = objectMapper.readValue(message, TransactionCompletedEvent.class);
            notificationService.handleTransactionCompleted(event);
        } catch (Exception e) {
            log.error("Error processing TransactionCompletedEvent", e);
        }
    }
}