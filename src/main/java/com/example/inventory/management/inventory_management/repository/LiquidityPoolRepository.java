package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.LiquidityPool;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiquidityPoolRepository extends JpaRepository<LiquidityPool, Long> {
    Optional<LiquidityPool> findByCurrency(String currency);

    @Query("SELECT p.currency FROM LiquidityPool p WHERE p.currency <> :currency ORDER BY p.balance ASC")
    List<String> findLowBalanceCurrency(@Param("currency") String currency, Pageable pageable);

    @Query("SELECT p.currency FROM LiquidityPool p WHERE p.currency <> :currency ORDER BY p.balance DESC")
    List<String> findHighBalanceCurrency(@Param("currency") String currency, Pageable pageable);
}

