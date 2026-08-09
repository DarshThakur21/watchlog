package com.datalog.watchlog.repository;

import com.datalog.watchlog.document.LogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.List;

public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, String> {

    List<LogDocument> findByServiceIdAndTimestampBetweenOrderByTimestampDesc(
            String serviceId, Instant from, Instant to);

    List<LogDocument> findByServiceIdAndLevelAndTimestampBetweenOrderByTimestampDesc(
            String serviceId, String level, Instant from, Instant to);

    List<LogDocument> findByMessageContainingOrderByTimestampDesc(String keyword);
}
