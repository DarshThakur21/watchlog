package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.HealthStatusResponse;
import com.datalog.watchlog.service.HealthCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Current health status of all registered services, for the dashboard.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthCheckService healthCheckService;

    @GetMapping
    public List<HealthStatusResponse> status() {
        return healthCheckService.currentStatus();
    }
}
