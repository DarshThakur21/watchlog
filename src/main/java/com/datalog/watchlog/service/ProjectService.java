package com.datalog.watchlog.service;

import com.datalog.watchlog.dto.ProjectRequest;
import com.datalog.watchlog.dto.ProjectResponse;
import com.datalog.watchlog.model.Projects;
import com.datalog.watchlog.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for projects. Entities never cross the API boundary — responses are
 * always mapped to {@link ProjectResponse}.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse create(ProjectRequest request) {
        if (projectRepository.existsByProjectName(request.projectName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project name already exists: " + request.projectName());
        }
        Projects project = Projects.builder()
                .projectName(request.projectName())
                .projectDescription(request.projectDescription() != null ? request.projectDescription() : "")
                .build();
        return toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> list() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProjectResponse get(UUID id) {
        return toResponse(find(id));
    }

    private Projects find(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found: " + id));
    }

    private ProjectResponse toResponse(Projects project) {
        return new ProjectResponse(
                project.getProjectId(),
                project.getProjectName(),
                project.getProjectDescription(),
                project.getCreatedAt().toInstant());
    }
}
