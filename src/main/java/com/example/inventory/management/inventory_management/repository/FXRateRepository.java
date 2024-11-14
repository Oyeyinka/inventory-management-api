package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.FXRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FXRateRepository extends JpaRepository<FXRate, Long> {
    Optional<FXRate> findByCurrencyPair(String currencyPair);

}
