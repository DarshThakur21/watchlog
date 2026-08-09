package com.datalog.watchlog.repository;

import com.datalog.watchlog.model.Projects;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Projects, UUID> {

    Optional<Projects> findByProjectName(String projectName);

    boolean existsByProjectName(String projectName);
}
