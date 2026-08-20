package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.LogIngestRequest;
import com.datalog.watchlog.dto.LogQueryRequest;
import com.datalog.watchlog.dto.LogQueryResponse;
import com.datalog.watchlog.service.LogIngestionService;
import com.datalog.watchlog.service.LogQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.rmi.server.UID;
import java.util.UUID;

/**
 * Log API: ingestion ({@code POST /api/logs}) and querying ({@code GET /api/logs}).
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogIngestionService ingestionService;
    private final LogQueryService queryService;

    @PostMapping
    public ResponseEntity<Void> ingest(@Valid @RequestBody LogIngestRequest request) {
        ingestionService.ingest(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping
    public LogQueryResponse query(@Valid @ModelAttribute LogQueryRequest request,@RequestParam (name = "serviceId") UUID serviceId) {
        LogQueryRequest updatedRequest = new LogQueryRequest(
                serviceId,
                request.level(),
                request.from(),
                request.to(),
                request.keyword(),
                request.page(),
                request.size()
        );
        return queryService.query(updatedRequest);
    }
}
