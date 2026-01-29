package com.banking.transactionservice.repository;

import com.banking.transactionservice.model.TransactionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    List<TransactionEvent> findByTransactionIdOrderByVersionAsc(String transactionId);
}