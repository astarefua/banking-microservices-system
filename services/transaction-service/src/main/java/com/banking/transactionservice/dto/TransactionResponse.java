package com.banking.transactionservice.dto;

import com.banking.transactionservice.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long id;
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private Transaction.TransactionType type;
    private BigDecimal amount;
    private String currency;
    private Transaction.TransactionStatus status;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}