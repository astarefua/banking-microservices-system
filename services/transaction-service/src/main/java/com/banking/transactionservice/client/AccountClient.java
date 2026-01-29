package com.banking.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "account-service")
public interface AccountClient {

    @PutMapping("/accounts/{accountNumber}/balance")
    void updateBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestParam("amount") BigDecimal amount
    );
}