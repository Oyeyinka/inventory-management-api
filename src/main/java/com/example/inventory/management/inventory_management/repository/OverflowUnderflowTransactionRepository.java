package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.OverflowUnderflowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OverflowUnderflowTransactionRepository extends JpaRepository<OverflowUnderflowTransaction, Long> {}

