package com.gesticar.backend.controller;

import com.gesticar.backend.domain.Customer;
import com.gesticar.backend.domain.Vehicle;
import com.gesticar.backend.domain.WorkOrder;
import com.gesticar.backend.dto.WorkOrderRequest;
import com.gesticar.backend.dto.WorkOrderStatusRequest;
import com.gesticar.backend.exception.ResourceNotFoundException;
import com.gesticar.backend.repository.CustomerRepository;
import com.gesticar.backend.repository.VehicleRepository;
import com.gesticar.backend.repository.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/ots")
public class WorkOrderController {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public WorkOrderController(WorkOrderRepository workOrderRepository, CustomerRepository customerRepository,
                               VehicleRepository vehicleRepository) {
        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public List<WorkOrder> findAll(@RequestParam(value = "customerId", required = false) Long customerId) {
        if (customerId != null) {
            return workOrderRepository.findByCustomerId(customerId);
        }
        return workOrderRepository.findAll();
    }

    @GetMapping("/{id}")
    public WorkOrder findById(@PathVariable Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OT no encontrada"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrder create(@Valid @RequestBody WorkOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Vehicle vehicle = request.vehicleId() != null
                ? vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado"))
                : null;

        WorkOrder workOrder = new WorkOrder();
        workOrder.setCode(request.code());
        workOrder.setDescription(request.description());
        if (request.status() != null) {
            workOrder.setStatus(request.status());
        }
        workOrder.setCustomer(customer);
        workOrder.setVehicle(vehicle);
        return workOrderRepository.save(workOrder);
    }

    @PatchMapping("/{id}/estado")
    public WorkOrder updateStatus(@PathVariable Long id, @Valid @RequestBody WorkOrderStatusRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OT no encontrada"));
        workOrder.setStatus(request.status());
        return workOrderRepository.save(workOrder);
    }
}
