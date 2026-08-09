package com.datalog.watchlog.repository;

import com.datalog.watchlog.model.HealthCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HealthCheckResultRepository extends JpaRepository<HealthCheckResult, UUID> {

    Optional<HealthCheckResult> findFirstByService_ServiceIdOrderByTimestampDesc(UUID serviceId);

    List<HealthCheckResult> findByService_ServiceIdAndTimestampBetweenOrderByTimestampDesc(
            UUID serviceId, LocalDateTime from, LocalDateTime to);

    List<HealthCheckResult> findByTimestampAfterOrderByTimestampAsc(LocalDateTime since);
}
