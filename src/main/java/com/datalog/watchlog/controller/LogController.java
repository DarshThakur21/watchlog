package com.datalog.watchlog.controller;

import com.datalog.watchlog.dto.LogIngestRequest;
import com.datalog.watchlog.dto.LogQueryRequest;
import com.datalog.watchlog.dto.LogQueryResponse;
import com.datalog.watchlog.service.LogIngestionService;
import com.datalog.watchlog.service.LogQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public LogQueryResponse query(@Valid LogQueryRequest request) {
        return queryService.query(request);
    }
}
