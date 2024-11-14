package com.example.inventory.management.inventory_management.repository;

import com.example.inventory.management.inventory_management.entity.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {
}
