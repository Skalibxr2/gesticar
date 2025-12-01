package com.gesticar.backend.repository;

import com.gesticar.backend.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByWorkOrderId(Long workOrderId);
}
