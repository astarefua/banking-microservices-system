package com.banking.transactionservice.kafka;

import com.banking.transactionservice.event.TransactionCompletedEvent;
import com.banking.transactionservice.event.TransactionCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TRANSACTION_CREATED_TOPIC = "transaction-created";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction-completed";

    public void publishTransactionCreated(TransactionCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TRANSACTION_CREATED_TOPIC, event.getTransactionId(), eventJson);
            log.info("Published TransactionCreatedEvent: {}", event.getTransactionId());
        } catch (JsonProcessingException e) {
            log.error("Error publishing TransactionCreatedEvent", e);
        }
    }

    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, event.getTransactionId(), eventJson);
            log.info("Published TransactionCompletedEvent: {}", event.getTransactionId());
        } catch (JsonProcessingException e) {
            log.error("Error publishing TransactionCompletedEvent", e);
        }
    }
}