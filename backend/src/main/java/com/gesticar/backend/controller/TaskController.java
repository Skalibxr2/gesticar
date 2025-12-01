package com.gesticar.backend.controller;

import com.gesticar.backend.domain.Task;
import com.gesticar.backend.domain.WorkOrder;
import com.gesticar.backend.dto.TaskRequest;
import com.gesticar.backend.exception.ResourceNotFoundException;
import com.gesticar.backend.repository.TaskRepository;
import com.gesticar.backend.repository.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tareas")
public class TaskController {

    private final TaskRepository taskRepository;
    private final WorkOrderRepository workOrderRepository;

    public TaskController(TaskRepository taskRepository, WorkOrderRepository workOrderRepository) {
        this.taskRepository = taskRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @GetMapping
    public List<Task> findAll(@RequestParam(value = "workOrderId", required = false) Long workOrderId) {
        if (workOrderId != null) {
            return taskRepository.findByWorkOrderId(workOrderId);
        }
        return taskRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task create(@Valid @RequestBody TaskRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(request.workOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("OT no encontrada"));
        Task task = new Task();
        task.setTitle(request.title());
        task.setDetails(request.details());
        task.setEstimatedHours(request.estimatedHours());
        task.setWorkOrder(workOrder);
        return taskRepository.save(task);
    }
}
