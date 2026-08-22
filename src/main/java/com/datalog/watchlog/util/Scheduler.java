package com.datalog.watchlog.util;

import com.datalog.watchlog.document.LogDocument;
import com.datalog.watchlog.repository.HealthCheckResultRepository;
import com.datalog.watchlog.repository.MetricPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class Scheduler {

    private final ElasticsearchOperations elasticsearchOperations;
    private final MetricPointRepository metricPointRepository;
    private final HealthCheckResultRepository healthCheckResultRepository;

    @Value("${watchlog.retention-days:1}")
    int retentionDays;

    @Value("${watchlog.retention-metric-days:1}")
    int metricRetentionDays;

    @Scheduled(cron = "0 0 22 * * *")
    public void deleteOlderThanRetention() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        Criteria criteria = new Criteria("timestamp").lessThan(cutoff.toString());
        CriteriaQuery searchQuery = new CriteriaQuery(criteria);

        DeleteQuery deleteQuery = DeleteQuery.builder(searchQuery).build();

        ByQueryResponse response = elasticsearchOperations.delete(deleteQuery, LogDocument.class);

        log.info("Log retention: deleted {} documents older than {} (retention={} days)",
                response.getDeleted(), cutoff, retentionDays);
    }


//    @Scheduled(cron = "30 9 22 * * *")
//    public void deleteOlderMetricPoints(){
//        LocalDateTime cutOff=LocalDateTime.now().minus(metricRetentionDays, ChronoUnit.DAYS);
//
//        int deleted=metricPointRepository.deleteByTimestampBefore(cutOff);
//        log.info("Metric retention: deleted {} metric points older than {} (retention={} days)",
//                deleted, cutOff, metricRetentionDays);
//
//    }

    @Scheduled(cron = "* */30 * * * *")
    @Transactional
    public void deleteOlderMetricPoints(){
        LocalDateTime cutOff=LocalDateTime.now();

        int deleted=metricPointRepository.deleteByTimestampBefore(cutOff);
        log.info("Metric retention: deleted {} metric points older than {} (retention={} days)",
                deleted, cutOff, metricRetentionDays);

    }

    @Scheduled(cron = "* */30 * * * *")
    @Transactional
    public void deleteHealthCheckResults(){
        LocalDateTime cutOff=LocalDateTime.now();

        int deleted=healthCheckResultRepository.deleteByTimeStampBefore(cutOff);
        log.info("Health check retention: deleted {} health check results older than today {}",deleted, cutOff);
    }





}