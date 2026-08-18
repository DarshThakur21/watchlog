package com.datalog.watchlog.service;

import com.datalog.watchlog.dto.HealthStatusResponse;
import com.datalog.watchlog.model.HealthCheckResult;
import com.datalog.watchlog.model.Services;
import com.datalog.watchlog.model.enums.ServiceStatus;
import com.datalog.watchlog.repository.HealthCheckResultRepository;
import com.datalog.watchlog.repository.ServicesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Periodically probes each registered service's health endpoint, records a
 * {@link HealthCheckResult}, and exposes the current status view for the dashboard.
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final ServicesRepository servicesRepository;
    private final HealthCheckResultRepository healthCheckResultRepository;
    private final RestClient restClient;

    public HealthCheckService(ServicesRepository servicesRepository,
                              HealthCheckResultRepository healthCheckResultRepository,
                              RestClient.Builder restClientBuilder) {
        this.servicesRepository = servicesRepository;
        this.healthCheckResultRepository = healthCheckResultRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 10_000)
    public void pollAll() {
        for (Services service : servicesRepository.findAll()) {
            checkService(service);
        }
    }

    public void checkService(Services service) {
        String url = service.getBaseUrl() + service.getHealthCheckEndpoint();
        long start = System.currentTimeMillis();
        ServiceStatus status;
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
            status = ServiceStatus.UP;
        } catch (Exception e) {
            status = ServiceStatus.DOWN;
        }
        long responseTimeMs = System.currentTimeMillis() - start;

        healthCheckResultRepository.save(HealthCheckResult.builder()
                .service(service)
                .status(status)
                .responseTimeMs(responseTimeMs)
                .build());
    }

    public List<HealthStatusResponse> currentStatus() {
        return servicesRepository.findAll().stream()
                .map(service -> {
                    HealthCheckResult latest = healthCheckResultRepository
                            .findFirstByService_ServiceIdOrderByTimestampDesc(service.getServiceId())
                            .orElse(null);
                    return new HealthStatusResponse(
                            service.getServiceId(),
                            latest != null ? latest.getStatus() : ServiceStatus.UNKNOWN,
                            service.getServiceName(),
                            latest != null ? latest.getTimestamp().toInstant(ZoneOffset.UTC) : null,
                            latest != null ? latest.getResponseTimeMs() : null);
                })
                .toList();
    }

    public HealthStatusResponse currentStatusByService(String serviceId) {
        return servicesRepository.findById(UUID.fromString(serviceId))
                .map(service -> {
                    HealthCheckResult latest = healthCheckResultRepository
                            .findFirstByService_ServiceIdOrderByTimestampDesc(service.getServiceId())
                            .orElse(null);
                    return new HealthStatusResponse(
                            service.getServiceId(),
                            latest != null ? latest.getStatus() : ServiceStatus.UNKNOWN,
                            service.getServiceName(),
                            latest != null ? latest.getTimestamp().toInstant(ZoneOffset.UTC) : null,
                            latest != null ? latest.getResponseTimeMs() : null);
                })
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

    }

}
