package com.gesticar.backend.repository;

import com.gesticar.backend.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
    List<WorkOrder> findByCustomerId(Long customerId);
}
