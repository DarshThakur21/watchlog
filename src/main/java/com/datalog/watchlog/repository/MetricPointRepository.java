package com.datalog.watchlog.repository;

import com.datalog.watchlog.model.MetricPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MetricPointRepository extends JpaRepository<MetricPoint, MetricPoint.MetricPointId> {

    List<MetricPoint> findByServiceIdAndMetricNameAndTimestampBetweenOrderByTimestampAsc(
            UUID serviceId, String metricName, LocalDateTime from, LocalDateTime to);

    List<MetricPoint> findByServiceIdOrderByTimestampDesc(UUID serviceId);

    /**
     * Bucketed aggregation for charts, using the TimescaleDB {@code time_bucket} function.
     * Requires the {@code metric_points} table to be a hypertable (Step 2).
     * Each row of the result is {@code Object[]{bucket (Timestamp), avg(value)}}.
     *
     * @param bucket interval text, e.g. {@code "1 minute"}
     */
    @Query(value = """
            SELECT time_bucket(:bucket::interval, timestamp) AS bucket, avg(value) AS avg_value
            FROM metric_points
            WHERE service_id = :serviceId AND metric_name = :metricName
              AND timestamp >= :from AND timestamp < :to
            GROUP BY bucket
            ORDER BY bucket ASC
            """, nativeQuery = true)
    List<Object[]> findBucketed(
            @Param("serviceId") UUID serviceId,
            @Param("metricName") String metricName,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("bucket") String bucket);

    @Modifying
    @Query("""
    DELETE FROM MetricPoint m
    WHERE m.timestamp < :cutOff
""")
    int deleteByTimestampBefore(LocalDateTime cutOff);
}
