package com.datalog.watchlog.model;

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
@Table(name = "metric_points" , indexes = {
// Crucial composite index for fast time-series graph querying
        @Index(name = "idx_service_metric_time", columnList = "service_id, metric_name, timestamp DESC")
})
public class MetricPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_point_id", updatable = false, nullable = false)
    private UUID metricPointId;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private Services service;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column(name = "value", nullable = false)
    private Double value;

}
