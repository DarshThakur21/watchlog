package com.datalog.watchlog.repository;

import com.datalog.watchlog.model.Services;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicesRepository extends JpaRepository<Services, UUID> {

    List<Services> findByProject_ProjectId(UUID projectId);

    Optional<Services> findByServiceIdAndProject_ProjectId(UUID serviceId, UUID projectId);

    boolean existsByServiceNameAndProject_ProjectId(String serviceName, UUID projectId);
}
