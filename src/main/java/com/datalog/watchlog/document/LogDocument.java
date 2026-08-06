package com.datalog.watchlog.document;


import com.datalog.watchlog.model.enums.LogLevel;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "application-logs")
public class LogDocument {

    @Id
    private String id;

    @Field
    private String projectId;

    private String serviceId;

    private Instant timestamp;

    private String level;

    private String logger;

    private String thread;

    private String message;

}
