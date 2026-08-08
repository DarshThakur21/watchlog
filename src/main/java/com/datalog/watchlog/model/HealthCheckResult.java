package com.datalog.watchlog.model;


import com.datalog.watchlog.model.enums.ServiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "health_check_results",indexes = {
        @Index(name = "idx_service_timestamp", columnList = "service_id, timestamp DESC")
})
public class HealthCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID healthCheckResultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Services service;

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;


}
