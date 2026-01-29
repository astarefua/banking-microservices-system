package com.banking.accountservice.dto;

import com.banking.accountservice.model.Account;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank(message = "Account holder name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String accountHolderName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Initial deposit must be greater than 0")
    private BigDecimal initialDeposit;

    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    private String currency = "USD";
}