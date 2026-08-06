package com.datalog.watchlog.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "services",indexes = {
        @Index(name = "idx_service_id", columnList = "service_id"),
        @Index(name = "idx_project_id", columnList = "project_id")
})
public class Services {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_id")
    private UUID serviceId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column (name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "health_check_endpoint", nullable = false)
    private String healthCheckEndpoint;

    @Column (name = "api_key", nullable = false)
    private String apiKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Projects project;

}
