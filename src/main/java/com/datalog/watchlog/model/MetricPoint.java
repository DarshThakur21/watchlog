package com.datalog.watchlog.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Time-series metric sample, stored as a TimescaleDB hypertable partitioned on
 * {@code timestamp}. The composite primary key includes the partition column,
 * which TimescaleDB requires for every unique index.
 */
@Entity
@Table(name = "metric_points", indexes = {
        @Index(name = "idx_service_metric_time", columnList = "service_id, metric_name, timestamp DESC")
})
@IdClass(MetricPoint.MetricPointId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricPoint {

    @Id
    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Id
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Id
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double value;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricPointId implements Serializable {

        private UUID serviceId;
        private String metricName;
        private LocalDateTime timestamp;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MetricPointId that)) return false;
            return Objects.equals(serviceId, that.serviceId)
                    && Objects.equals(metricName, that.metricName)
                    && Objects.equals(timestamp, that.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceId, metricName, timestamp);
        }
    }
}
