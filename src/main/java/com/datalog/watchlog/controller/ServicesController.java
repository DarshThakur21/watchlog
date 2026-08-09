package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.ServiceRequest;
import com.datalog.watchlog.dto.ServiceResponse;
import com.datalog.watchlog.service.ServicesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
}
