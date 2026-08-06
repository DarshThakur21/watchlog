package com.datalog.watchlog.document;


import com.datalog.watchlog.model.enums.LogLevel;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "application-logs")
public class LogDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "project_id")
    private String projectId;

    @Field(type = FieldType.Keyword, name = "service_id")
    private String serviceId;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time, name = "timestamp")
    private Instant timestamp;

    @Field(type = FieldType.Keyword, name = "level")
    private String level;

    @Field(type = FieldType.Keyword, name = "logger")
    private String logger;

    @Field(type = FieldType.Keyword, name = "thread")
    private String thread;

    @Field(type = FieldType.Text, name = "message")
    private String message;

}
