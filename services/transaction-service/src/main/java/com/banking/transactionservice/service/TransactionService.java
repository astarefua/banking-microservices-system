package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountClient;
import com.banking.transactionservice.dto.TransactionRequest;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.event.TransactionCompletedEvent;
import com.banking.transactionservice.event.TransactionCreatedEvent;
import com.banking.transactionservice.exception.InsufficientFundsException;
import com.banking.transactionservice.exception.TransactionNotFoundException;
import com.banking.transactionservice.kafka.TransactionEventProducer;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionEvent;
import com.banking.transactionservice.repository.TransactionEventRepository;
import com.banking.transactionservice.repository.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final TransactionEventProducer eventProducer;
    private final AccountClient accountClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        log.info("Creating transaction: {} from account {}", request.getType(), request.getFromAccount());

        // Validate transaction type
        validateTransactionRequest(request);

        // Generate unique transaction ID
        String transactionId = UUID.randomUUID().toString();

        // Create transaction entity
        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .fromAccount(request.getFromAccount())
                .toAccount(request.getToAccount())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(Transaction.TransactionStatus.PENDING)
                .description(request.getDescription())
                .build();

        // Save transaction
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Store event (Event Sourcing)
        saveTransactionEvent(transactionId, "TRANSACTION_CREATED", savedTransaction);

        // Publish event to Kafka
        publishTransactionCreatedEvent(savedTransaction);

        log.info("Transaction created: {}", transactionId);

        // Process the transaction asynchronously (in real-world, this would be async)
        processTransaction(transactionId);

        return mapToResponse(savedTransaction);
    }

    @Transactional
    public void processTransaction(String transactionId) {
        log.info("Processing transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));

        // Update status to PROCESSING
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transactionRepository.save(transaction);
        saveTransactionEvent(transactionId, "TRANSACTION_PROCESSING", transaction);

        try {
            // Execute the transaction based on type
            switch (transaction.getType()) {
                case DEPOSIT -> executeDeposit(transaction);
                case WITHDRAWAL -> executeWithdrawal(transaction);
                case TRANSFER -> executeTransfer(transaction);
            }

            // Mark as completed
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            saveTransactionEvent(transactionId, "TRANSACTION_COMPLETED", transaction);

            // Publish completion event
            publishTransactionCompletedEvent(transaction);

            log.info("Transaction completed successfully: {}", transactionId);

        } catch (Exception e) {
            log.error("Transaction failed: {}", transactionId, e);
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setFailureReason(e.getMessage());
            transactionRepository.save(transaction);
            saveTransactionEvent(transactionId, "TRANSACTION_FAILED", transaction);
            throw e;
        }
    }

    private void executeDeposit(Transaction transaction) {
        log.info("Executing deposit: {} to account {}", transaction.getAmount(), transaction.getFromAccount());

        // Call Account Service to update balance
        accountClient.updateBalance(transaction.getFromAccount(), transaction.getAmount());
    }

    private void executeWithdrawal(Transaction transaction) {
        log.info("Executing withdrawal: {} from account {}", transaction.getAmount(), transaction.getFromAccount());

        // Deduct from account (negative amount)
        accountClient.updateBalance(transaction.getFromAccount(), transaction.getAmount().negate());
    }

    private void executeTransfer(Transaction transaction) {
        log.info("Executing transfer: {} from {} to {}",
                transaction.getAmount(), transaction.getFromAccount(), transaction.getToAccount());

        // Deduct from source account
        accountClient.updateBalance(transaction.getFromAccount(), transaction.getAmount().negate());

        // Add to destination account
        accountClient.updateBalance(transaction.getToAccount(), transaction.getAmount());
    }

    public TransactionResponse getTransactionById(Long id) {
        log.info("Fetching transaction by ID: {}", id);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + id));
        return mapToResponse(transaction);
    }

    public TransactionResponse getTransactionByTransactionId(String transactionId) {
        log.info("Fetching transaction by transaction ID: {}", transactionId);
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found: " + transactionId));
        return mapToResponse(transaction);
    }

    public List<TransactionResponse> getTransactionsByAccount(String accountNumber) {
        log.info("Fetching transactions for account: {}", accountNumber);
        List<Transaction> fromTransactions = transactionRepository.findByFromAccount(accountNumber);
        List<Transaction> toTransactions = transactionRepository.findByToAccount(accountNumber);

        // Combine both lists
        fromTransactions.addAll(toTransactions);

        return fromTransactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getAllTransactions() {
        log.info("Fetching all transactions");
        return transactionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TransactionEvent> getTransactionEvents(String transactionId) {
        log.info("Fetching transaction events for: {}", transactionId);
        return transactionEventRepository.findByTransactionIdOrderByVersionAsc(transactionId);
    }

    // Helper methods
    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getType() == Transaction.TransactionType.TRANSFER &&
                (request.getToAccount() == null || request.getToAccount().isBlank())) {
            throw new IllegalArgumentException("To account is required for transfer transactions");
        }
    }

    private void saveTransactionEvent(String transactionId, String eventType, Transaction transaction) {
        try {
            String eventData = objectMapper.writeValueAsString(transaction);

            // Get current version (count of existing events + 1)
            long version = transactionEventRepository.findByTransactionIdOrderByVersionAsc(transactionId).size() + 1;

            TransactionEvent event = TransactionEvent.builder()
                    .transactionId(transactionId)
                    .eventType(eventType)
                    .eventData(eventData)
                    .version(version)
                    .build();

            transactionEventRepository.save(event);
            log.debug("Transaction event saved: {} - {}", transactionId, eventType);

        } catch (JsonProcessingException e) {
            log.error("Error saving transaction event", e);
        }
    }

    private void publishTransactionCreatedEvent(Transaction transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.publishTransactionCreated(event);
    }

    private void publishTransactionCompletedEvent(Transaction transaction) {
        TransactionCompletedEvent event = TransactionCompletedEvent.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .timestamp(LocalDateTime.now())
                .build();

        eventProducer.publishTransactionCompleted(event);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .failureReason(transaction.getFailureReason())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}