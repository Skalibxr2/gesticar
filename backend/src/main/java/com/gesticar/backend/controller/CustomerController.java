package com.gesticar.backend.controller;

import com.gesticar.backend.domain.Customer;
import com.gesticar.backend.dto.CustomerRequest;
import com.gesticar.backend.exception.ResourceNotFoundException;
import com.gesticar.backend.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<Customer> findAll(@RequestParam(value = "rut", required = false) String rut) {
        if (rut != null && !rut.isBlank()) {
            return customerRepository.findByRut(rut).map(List::of).orElse(List.of());
        }
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer findById(@PathVariable Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@Valid @RequestBody CustomerRequest request) {
        Customer customer = new Customer();
        customer.setRut(request.rut());
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        return customerRepository.save(customer);
    }
}
