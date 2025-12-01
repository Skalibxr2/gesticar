package com.gesticar.backend.repository;

import com.gesticar.backend.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByWorkOrderId(Long workOrderId);
}
