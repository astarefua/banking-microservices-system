package com.banking.accountservice.service;

import com.banking.accountservice.dto.AccountRequest;
import com.banking.accountservice.dto.AccountResponse;
import com.banking.accountservice.exception.AccountNotFoundException;
import com.banking.accountservice.model.Account;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        log.info("Creating new account for: {}", request.getAccountHolderName());

        // Check if email already exists
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Account with this email already exists");
        }

        // Generate unique account number
        String accountNumber = generateAccountNumber();

        // Create account entity
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhoneNumber(request.getPhoneNumber());
        account.setAccountType(request.getAccountType());
        account.setBalance(request.getInitialDeposit());
        account.setCurrency(request.getCurrency());
        account.setStatus(Account.AccountStatus.ACTIVE);

        // Save to database
        Account savedAccount = accountRepository.save(account);
        log.info("Account created successfully: {}", savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }

    public AccountResponse getAccountById(Long id) {
        log.info("Fetching account by ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));
        return mapToResponse(account);
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account by account number: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
        return mapToResponse(account);
    }

    public List<AccountResponse> getAllAccounts() {
        log.info("Fetching all accounts");
        return accountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateBalance(String accountNumber, BigDecimal amount) {
        log.info("Updating balance for account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);

        log.info("Balance updated successfully for account: {}", accountNumber);
        return mapToResponse(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        log.info("Deleting account with ID: {}", id);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + id));

        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Account closed successfully: {}", account.getAccountNumber());
    }

    // Helper method to generate unique account number
    private String generateAccountNumber() {
        Random random = new Random();
        String accountNumber;
        do {
            accountNumber = String.format("%010d", random.nextInt(1000000000));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    // Helper method to map Account entity to AccountResponse DTO
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountHolderName(account.getAccountHolderName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}