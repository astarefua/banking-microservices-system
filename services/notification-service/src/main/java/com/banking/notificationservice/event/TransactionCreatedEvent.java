package com.banking.notificationservice.event;

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
public class TransactionCreatedEvent {

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime timestamp;
}