package com.datalog.watchlog.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bucketed time-series for a single metric, for charts.
 *
 * @param serviceId  the service the metric belongs to
 * @param metricName the metric name (e.g. {@code cpu_usage})
 * @param points     one value per time bucket, ordered by timestamp
 */
public record MetricQueryResponse(
        UUID serviceId,
        String metricName,
        List<Point> points) {

    /**
     * A single time-bucket sample.
     *
     * @param timestamp start of the bucket
     * @param value     aggregated value in the bucket (e.g. avg)
     */
    public record Point(Instant timestamp, Double value) {
    }
}
