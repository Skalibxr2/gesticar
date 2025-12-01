package com.gesticar.backend.controller;

import com.gesticar.backend.domain.Customer;
import com.gesticar.backend.domain.Vehicle;
import com.gesticar.backend.dto.VehicleRequest;
import com.gesticar.backend.exception.ResourceNotFoundException;
import com.gesticar.backend.repository.CustomerRepository;
import com.gesticar.backend.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    public VehicleController(VehicleRepository vehicleRepository, CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<Vehicle> findAll(@RequestParam(value = "customerId", required = false) Long customerId) {
        if (customerId != null) {
            return vehicleRepository.findByCustomerId(customerId);
        }
        return vehicleRepository.findAll();
    }

    @GetMapping("/{id}")
    public Vehicle findById(@PathVariable Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Vehicle create(@Valid @RequestBody VehicleRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.licensePlate());
        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setCustomer(customer);
        return vehicleRepository.save(vehicle);
    }
}
