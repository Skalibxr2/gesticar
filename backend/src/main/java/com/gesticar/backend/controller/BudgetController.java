package com.gesticar.backend.controller;

import com.gesticar.backend.domain.Budget;
import com.gesticar.backend.domain.WorkOrder;
import com.gesticar.backend.dto.BudgetRequest;
import com.gesticar.backend.exception.ResourceNotFoundException;
import com.gesticar.backend.repository.BudgetRepository;
import com.gesticar.backend.repository.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final WorkOrderRepository workOrderRepository;

    public BudgetController(BudgetRepository budgetRepository, WorkOrderRepository workOrderRepository) {
        this.budgetRepository = budgetRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @GetMapping
    public List<Budget> findAll(@RequestParam(value = "workOrderId", required = false) Long workOrderId) {
        if (workOrderId != null) {
            return budgetRepository.findByWorkOrderId(workOrderId);
        }
        return budgetRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Budget create(@Valid @RequestBody BudgetRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(request.workOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("OT no encontrada"));
        Budget budget = new Budget();
        budget.setAmount(request.amount());
        budget.setApproved(Boolean.TRUE.equals(request.approved()));
        budget.setNotes(request.notes());
        budget.setWorkOrder(workOrder);
        return budgetRepository.save(budget);
    }
}
