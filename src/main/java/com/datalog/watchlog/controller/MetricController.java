package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.MetricQueryResponse;
import com.datalog.watchlog.service.MetricQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Bucketed time-series for a metric, for charts.
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricQueryService metricQueryService;

    @GetMapping
    public MetricQueryResponse query(@RequestParam UUID serviceId,
                                     @RequestParam String metricName,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to,
                                     @RequestParam(required = false, defaultValue = "1 minute") String bucket) {
        return metricQueryService.query(serviceId, metricName, from, to, bucket);
    }
}
