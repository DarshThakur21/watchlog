package com.datalog.watchlog.service;

import com.datalog.watchlog.dto.MetricQueryResponse;
import com.datalog.watchlog.repository.MetricPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Read side for metrics: runs the TimescaleDB {@code time_bucket} aggregation and
 * returns a {@link MetricQueryResponse} for charts.
 */
@Service
@RequiredArgsConstructor
public class MetricQueryService {

    private final MetricPointRepository metricPointRepository;

    public MetricQueryResponse query(UUID serviceId, String metricName, Instant from, Instant to, String bucket) {
        LocalDateTime effectiveFrom = from != null
                ? from.atZone(ZoneOffset.UTC).toLocalDateTime()
                : LocalDateTime.now(ZoneOffset.UTC).minusHours(1);

        LocalDateTime effectiveTo = to != null
                ? to.atZone(ZoneOffset.UTC).toLocalDateTime()
                : LocalDateTime.now(ZoneOffset.UTC);

        String bucketInterval = bucket != null && !bucket.isBlank() ? bucket : "1 minute";

        List<Object[]> rows = metricPointRepository.findBucketed(serviceId, metricName, effectiveFrom, effectiveTo, bucketInterval);

        List<MetricQueryResponse.Point> points = rows.stream()
                .map(row -> new MetricQueryResponse.Point(
                        ((Timestamp) row[0]).toInstant(),
                        ((Number) row[1]).doubleValue()))
                .toList();

        return new MetricQueryResponse(serviceId, metricName, points);
    }
}
