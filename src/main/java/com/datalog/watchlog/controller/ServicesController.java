package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.ServiceRequest;
import com.datalog.watchlog.dto.ServiceResponse;
import com.datalog.watchlog.service.ServicesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Service registration API.
 */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServicesController {

    private final ServicesService servicesService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody ServiceRequest request) {
        return servicesService.create(request);
    }

    @GetMapping
    public List<ServiceResponse> list(@RequestParam(required = false) UUID projectId) {
        return projectId != null ? servicesService.listByProject(projectId) : servicesService.listAll();
    }

    @GetMapping("/{id}")
    public ServiceResponse get(@PathVariable UUID id) {
        return servicesService.get(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable  UUID id) {
        servicesService.deleteService(id);
    }
}
