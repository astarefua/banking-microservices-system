package com.banking.accountservice.dto;

import com.banking.accountservice.model.Account;
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
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private String phoneNumber;
    private Account.AccountType accountType;
    private BigDecimal balance;
    private Account.AccountStatus status;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}