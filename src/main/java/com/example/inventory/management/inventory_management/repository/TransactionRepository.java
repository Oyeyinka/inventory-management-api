package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.toCurrency = :currency AND t.timestamp > :startTime")
    BigDecimal calculateRecentVolume(@Param("currency") String currency, @Param("startTime") LocalDateTime startTime);

}
