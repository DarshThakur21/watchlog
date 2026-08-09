package com.datalog.watchlog.service;

import com.datalog.watchlog.model.MetricPoint;
import com.datalog.watchlog.model.Services;
import com.datalog.watchlog.repository.MetricPointRepository;
import com.datalog.watchlog.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;

/**
 * Samples synthetic metrics for every registered service and writes {@link MetricPoint}
 * rows into the TimescaleDB hypertable. Swap the synthetic values for a real scrape
 * (e.g. Micrometer / Prometheus) later.
 */
@Service
@RequiredArgsConstructor
public class MetricPointPoller {

    private final ServicesRepository servicesRepository;
    private final MetricPointRepository metricPointRepository;
    private final Random random = new Random();

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void sampleAll() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        for (Services service : servicesRepository.findAll()) {
            sample(service.getServiceId(), now);
        }
    }

    private void sample(UUID serviceId, LocalDateTime now) {
        metricPointRepository.save(MetricPoint.builder()
                .serviceId(serviceId)
                .metricName("cpu_usage")
                .timestamp(now)
                .value(10 + random.nextDouble() * 80)
                .build());
        metricPointRepository.save(MetricPoint.builder()
                .serviceId(serviceId)
                .metricName("memory_usage_mb")
                .timestamp(now)
                .value(128 + random.nextDouble() * 512)
                .build());
    }
}
