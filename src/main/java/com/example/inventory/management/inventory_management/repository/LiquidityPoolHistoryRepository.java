package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.LiquidityPoolHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiquidityPoolHistoryRepository extends JpaRepository<LiquidityPoolHistory, Long> {
}
