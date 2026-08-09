package com.datalog.watchlog.service;

import com.datalog.watchlog.document.LogDocument;
import com.datalog.watchlog.dto.LogQueryRequest;
import com.datalog.watchlog.dto.LogQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read side of the log pipeline: runs a dynamic, filterable search against the
 * {@code application-logs} index and returns a paginated response.
 *
 * <p>Criteria field names are the Elasticsearch document field names (the
 * {@code @Field(name = ...)} values), not the Java property names.
 */
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private final ElasticsearchOperations operations;

    public LogQueryResponse query(LogQueryRequest request) {
        Criteria criteria = new Criteria();

        if (request.serviceId() != null) {
            criteria.and("service_id").is(request.serviceId().toString());
        }
        if (request.level() != null) {
            criteria.and("level").is(request.level().name());
        }
        if (request.from() != null || request.to() != null) {
            Criteria time = new Criteria("timestamp");
            if (request.from() != null) {
                time.greaterThanEqual(request.from());
            }
            if (request.to() != null) {
                time.lessThan(request.to());
            }
            criteria.and(time);
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            criteria.and("message").matches(request.keyword().trim());
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(request.page(), request.size()));
        query.addSort(Sort.by(Sort.Order.desc("timestamp")));

        SearchHits<LogDocument> hits = operations.search(query, LogDocument.class);
        List<LogDocument> logs = hits.getSearchHits().stream().map(SearchHit::getContent).toList();
        return new LogQueryResponse(logs, hits.getTotalHits(), request.page());
    }
}
