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
public class TransactionCompletedEvent {

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private String type;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
}