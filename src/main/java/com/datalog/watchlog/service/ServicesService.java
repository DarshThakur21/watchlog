package com.datalog.watchlog.service;

import com.datalog.watchlog.dto.ServiceRequest;
import com.datalog.watchlog.dto.ServiceResponse;
import com.datalog.watchlog.model.Projects;
import com.datalog.watchlog.model.Services;
import com.datalog.watchlog.repository.ProjectRepository;
import com.datalog.watchlog.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for services. Entities never cross the API boundary — responses are
 * always mapped to {@link ServiceResponse}, which omits the {@code apiKey}.
 */
@Service
@RequiredArgsConstructor
public class ServicesService {

    private final ServicesRepository servicesRepository;
    private final ProjectRepository projectRepository;

    public ServiceResponse create(ServiceRequest request) {
        Projects project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + request.projectId()));
        if (servicesRepository.existsByServiceNameAndProject_ProjectId(request.serviceName(), request.projectId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Service name already exists in project: " + request.serviceName());
        }

        Services service = Services.builder()
                .serviceName(request.serviceName())
                .baseUrl(request.baseUrl())
                .healthCheckEndpoint(request.healthCheckEndpoint())
                .apiKey(request.apiKey()) // auto-generated UUID by @PrePersist when null
                .project(project)
                .build();
        return toResponse(servicesRepository.save(service));
    }

    public List<ServiceResponse> listAll() {
        return servicesRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ServiceResponse> listByProject(UUID projectId) {
        return servicesRepository.findByProject_ProjectId(projectId).stream().map(this::toResponse).toList();
    }

    public ServiceResponse get(UUID id) {
        return toResponse(servicesRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found: " + id)));
    }

    private ServiceResponse toResponse(Services service) {
        return new ServiceResponse(
                service.getServiceId(),
                service.getProject().getProjectId(),
                service.getServiceName(),
                service.getBaseUrl(),
                service.getHealthCheckEndpoint(),
                service.getCreatedAt().toInstant());
    }
}
