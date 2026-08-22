# Watchlog — Project Roadmap

Kafka-based log pipeline with TimescaleDB metrics integration. Stack: Spring Boot 4.1.0 (Java 21), TimescaleDB (Postgres 16), Elasticsearch 9.4.2, Kafka (KRaft single-node).

> **Current infra ports (as actually configured in `dockcompos/docker-compose.yml`):** Postgres **5444**, Elasticsearch **9200**, Kafka **9000**. DB name **`watchlog_db`**, user `root`, password `root`. Backend app runs on **7000**.
> **Note:** Elasticsearch was bumped from 8.13 → **9.4.2** to match Spring Data Elasticsearch 6.1 (see Error Fix Log #9).

---

## ✅ Step 0 — Infrastructure (DONE)

Docker services up:

| Service | Port | Container |
|---|---|---|
| TimescaleDB (Postgres 16) | 5444 (→5432) | `watchlog-postgres` |
| Elasticsearch 9.4.2 | 9200 | `watchlog-elasticsearch` |
| Kafka (KRaft) | 9000 (→9092) | `watchlog-kafka` |

DB credentials: database `watchlog_db`, user `root`, password `root`.

---

## 🎯 Step 1 — Domain models ("entities")

Create the model files under `com.datalog.watchlog`, split by storage system.

### Package `enums`

| File | Contents |
|---|---|
| `LogLevel` | `TRACE, DEBUG, INFO, WARN, ERROR` |
| `ServiceStatus` | `UP, DOWN, UNKNOWN` |

### Package `entity` (JPA → Postgres / TimescaleDB)

| File | Fields | Notes |
|---|---|---|
| `Project` | `id, name, createdAt` | `@Entity`; name unique; `createdAt` set on persist |
| `Service` | `id, project (FK→Project), name, baseUrl, healthPath, apiKey, createdAt` | unique (`project_id, name`); `apiKey` auto-generated UUID on persist |
| `HealthCheckResult` | `id, service (FK→Service), timestamp, status (ServiceStatus), responseTimeMs` | index on (`service_id, timestamp`) |
| `MetricPoint` | `serviceId, metricName, timestamp, value` | composite key via `@IdClass`; becomes the TimescaleDB **hypertable** |

### Package `document` (Spring Data ES → Elasticsearch)

| File | Fields |
|---|---|
| `LogDocument` | `@Id id, serviceId, projectId, timestamp, level, logger, thread, message` |

### Package `event` (Kafka payload)

| File | Fields |
|---|---|
| `LogEventMessage` | `serviceId, projectId, timestamp, level, logger, thread, message` — plain `record`, same shape as `LogDocument` minus the ES id |

### Package `dto` (REST request/response — kept separate from entities)

| File | Purpose |
|---|---|
| `ProjectRequest` / `ProjectResponse` | create project / return `id, name, createdAt` |
| `ServiceRequest` / `ServiceResponse` | request has `projectId, name, baseUrl, healthPath`; response **omits apiKey** |
| `LogIngestRequest` | `serviceId, timestamp, level, logger, thread, message` (the appender's POST body) |
| `LogQueryRequest` | `serviceId, level, from, to, keyword, page, size` |
| `LogQueryResponse` | `logs (List<LogDocument>), totalHits, page` |
| `HealthStatusResponse` | `serviceId, status, lastCheckedAt, responseTimeMs` |
| `MetricQueryResponse` | `serviceId, metricName, points (List<{timestamp, value}>)` |

### Also in this step

Fill in `application.properties` so the app boots against the running containers:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/watchlog`, user/pass `root/root`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.elasticsearch.uris=http://localhost:9200`
- `spring.kafka.bootstrap-servers=localhost:9092`

---

## Step 2 — DB schema & TimescaleDB hypertable

After `ddl-auto` creates the tables, run once against Postgres:

```sql
CREATE EXTENSION IF NOT EXISTS timescaledb;
SELECT create_hypertable('metric_points', 'timestamp');
```

Add supporting indexes (e.g. `health_check_result`, `metric_points` composite). Note: the `metric_points` table is created automatically by JPA on first boot — only the extension + hypertable conversion is manual.

---

## Step 3 — Repositories

JPA: `ProjectRepository`, `ServiceRepository`, `HealthCheckResultRepository`, `MetricPointRepository`.

Elasticsearch: `LogDocumentRepository extends ElasticsearchRepository<LogDocument, String>` with derived query methods for filtering.

---

## Step 4 — Kafka wiring

Configure producer + consumer in `application.properties` (JSON serializer/deserializer, trust-package). Enables the decoupled ingestion flow:

- `POST /api/logs` → validate → produce `LogEventMessage` to topic **`log-events`**
- `@KafkaListener(topics = "log-events", groupId = "log-indexer")` → write `LogDocument` to ES

---

## Step 5 — Ingestion API ✅

`POST /api/logs` controller + service (`LogController`, `LogIngestionService`): validates `LogIngestRequest`, resolves project from `serviceId`, produces `LogEventMessage` to `log-events`. No direct ES write — Kafka buffers it. Returns `202 Accepted`.

---

## Step 6 — Log indexer consumer ✅

Consumer service (`LogIndexerService`) that reads `log-events`, maps `LogEventMessage → LogDocument` (via `toDocument()`), saves to ES. This is where the pipeline's decoupling lives.

---

## Step 7 — Log query API ✅

`GET /api/logs` with `serviceId, level, from, to, keyword, page, size` filters → dynamic `CriteriaQuery` via `ElasticsearchOperations` → paginated `LogQueryResponse`. (`LogController` + `LogQueryService`; Criteria uses ES document field names.)

---

## Step 8 — Health poller ✅

Scheduled job (`HealthCheckService`, every 30s via `@EnableScheduling`) hitting each `Service.baseUrl + healthCheckEndpoint` with a 3s-timeout `RestClient` → stores `HealthCheckResult` → `GET /api/health` exposes current status as `List<HealthStatusResponse>`.

---

## Step 9 — Metrics poller + queries ✅

Scheduled polling (`MetricPointPoller`, every 15s) writes `MetricPoint` rows (`cpu_usage`, `memory_usage_mb`); `GET /api/metrics` uses `time_bucket()` aggregation (`MetricQueryService` + `MetricPointRepository.findBucketed`) → `MetricQueryResponse` for charts.

---

## Step 10 — WebSocket live streaming

Push log events / health changes to the browser in real time over WebSocket (`spring-boot-starter-websocket`).

---

## Step 11 — REST layer & DTO mapping ✅

`ProjectController`/`ProjectService` + `ServicesController`/`ServicesService` (create/list/get; uniqueness → 409, missing → 404). `HealthController`/`MetricController` from Steps 8–9. **Rule:** entities never leave the service layer — always map `Entity → Response DTO` (and `apiKey` is never echoed back).

---

## Step 12 — Appender library (separate small project)

**Goal:** stream the `upi-wallet-system` microservices' logs into watchlog for display.

New standalone Maven project **`watchlog-appender/`** (sibling of the backend; Java 17; zero transitive deps):
- `LogEventPayload` — public record mirroring the `POST /api/logs` ingest contract
- `WatchlogHttpAppender` — Logback appender (`UnsynchronizedAppenderBase<ILoggingEvent>`): async bounded queue (10k) + daemon sender, drain-on-stop, recursion guard for its own package, optional `X-API-Key` header, JDK `HttpClient`, 2s timeouts
- `JsonWriter` — hand-rolled minimal JSON serializer (correct escaping, `null` for absent logger/thread/message)

Integration per consuming service: add the Maven dep → register the service in watchlog (get its UUID) → add a `logback-spring.xml` with the appender (`<url>` + `<serviceId>`). Containers reach the host-run watchlog via `host.docker.internal:7000`.

Deliberately NOT a shared dependency on the platform's DTO module — it only mirrors the JSON contract.

---

## Step 13 (optional) — Extras

- Kafka UI container (`provectuslabs/kafka-ui`) for inspecting topics
- Frontend / query UI

---

### Design notes (from history)

- Entities never leak over the API — controllers map `Project`/`Service` entities to their Request/Response DTOs. Prevents leaking `apiKey` and coupling the DB schema to the public API shape.
- `LogEventPayload` in the appender library is standalone on purpose — microservices shouldn't depend on the platform's codebase just to log.
- If the app later gets containerized into the same compose file, add a second Kafka listener advertised as `watchlog-kafka:9092` for container-to-container traffic.

---

# 📦 Complete Implementation Record

The definitive record of what was actually built — files, field names, flow, and why. Everything lives under `src/main/java/com/datalog/watchlog/`.

## 1. Package & file map

```
com.datalog.watchlog
├── WatchlogApplication.java        @SpringBootApplication + @EnableScheduling
├── config/KafkaConfig.java         topic + JSON producer/consumer (dedicated ObjectMapper)
├── controller/
│   ├── LogController.java          POST+GET /api/logs
│   ├── ProjectController.java      /api/projects
│   ├── ServicesController.java     /api/services
│   ├── HealthController.java       /api/health
│   └── MetricController.java       /api/metrics
├── dto/                            REST request/response records (never entities)
│   ├── ProjectRequest / ProjectResponse
│   ├── ServiceRequest / ServiceResponse
│   ├── LogIngestRequest / LogQueryRequest / LogQueryResponse
│   ├── HealthStatusResponse
│   └── MetricQueryResponse (+ nested Point)
├── document/LogDocument.java       Elasticsearch @Document("application-logs")
├── event/LogEventMessage.java      Kafka record + toDocument() helper
├── model/                          JPA entities (Postgres / TimescaleDB)
│   ├── Projects.java               @Table("projects")
│   ├── Services.java               @Table("services")
│   ├── HealthCheckResult.java      @Table("health_check_results")
│   ├── MetricPoint.java            @Table("metric_points") composite @IdClass
│   └── enums/LogLevel.java, enums/ServiceStatus.java
├── repository/                     Spring Data interfaces
│   ├── ProjectRepository, ServicesRepository, HealthCheckResultRepository,
│   │   MetricPointRepository       (JPA — incl. native time_bucket query)
│   └── LogDocumentRepository       (Elasticsearch)
└── service/
    ├── LogIngestionService         validate + produce to Kafka
    ├── LogIndexerService           @KafkaListener → ES
    ├── LogQueryService             dynamic CriteriaQuery → paginated search
    ├── HealthCheckService          @Scheduled 30s probe → HealthCheckResult
    ├── MetricPointPoller           @Scheduled 15s synthetic samples
    ├── MetricQueryService          time_bucket aggregation
    ├── ProjectService, ServicesService   CRUD + Entity→DTO mapping
```

**Key rule (from history):** the `dto/` records never equal the JPA entities. Controllers map `Entity → Response DTO` so `apiKey` and internal state never cross the API boundary.

---

## 2. Data model (Step 1)

### Enums — `model/enums`
- `LogLevel` → `TRACE, DEBUG, INFO, WARN, ERROR`
- `ServiceStatus` → `UP, DOWN, UNKNOWN`

### JPA entities
**`Projects`** — `projects` table: `projectId (UUID, PK)`, `projectName (unique, not null)`, `projectDescription`, `createdAt (Date, @CreationTimestamp, updatable=false)`.

**`Services`** — `services` table: `serviceId (UUID, PK)`, `serviceName`, `baseUrl`, `healthCheckEndpoint`, `apiKey` (**auto-generated UUID via `@PrePersist` if omitted**), `createdAt`, `@ManyToOne project`. Uniqueness: `(project_id, service_name)`. Indexes on `service_id`, `project_id`.

**`HealthCheckResult`** — `health_check_results` table: `healthCheckResultId (UUID, PK)`, `@ManyToOne(LAZY) service`, `timestamp (@CreationTimestamp, LocalDateTime)`, `status (@Enumerated(STRING))`, `responseTimeMs`. Index `(service_id, timestamp DESC)`.

**`MetricPoint`** — `metric_points` table: **composite PK** `(serviceId, metricName, timestamp)` via `@IdClass(MetricPointId.class)` + plain `value (Double)`. The composite key includes the partition column `timestamp` — a hard requirement of TimescaleDB `create_hypertable()`. Index `(service_id, metric_name, timestamp DESC)`. `timestamp` is the poller-set sample time (not `@CreationTimestamp`).

### Elasticsearch document — `document/LogDocument`
`@Document(indexName = "application-logs")`. Field names (ES names in parens): `id (@Id, String → ES _id)`, `projectId (project_id, Keyword)`, `serviceId (service_id, Keyword)`, `timestamp (date_optional_time)`, `level (Keyword)`, `logger (Keyword)`, `thread (Keyword)`, `message (Text)`. Uses **`org.springframework.data.annotation.Id`** — not JPA's `@Id` (Error Fix #1).

### Kafka event — `event/LogEventMessage`
A `record(String serviceId, String projectId, Instant timestamp, String level, String logger, String thread, String message)` with a `toDocument()` helper that maps straight into a `LogDocument`. IDs are **String** (JSON over Kafka / ES), while JPA uses UUID.

### DTOs — `dto/`
| DTO                    | Fields                                                                                                                              | Notes                                 |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------|
| `ProjectRequest`       | `projectName (@NotBlank)`, `projectDescription`                                                                                     | description stored as `""` if omitted |
| `ProjectResponse`      | `id (UUID)`, `projectName`, `projectDescription`, `createdAt (Instant)`                                                             |                                       |
| `ServiceRequest`       | `projectId (@NotNull UUID)`, `serviceName (@NotBlank)`, `baseUrl`, `healthCheckEndpoint`, `apiKey`                                  | apiKey optional (auto-gen)            |
| `ServiceResponse`      | `id`, `projectId`, `serviceName`, `baseUrl`, `healthCheckEndpoint`, `createdAt`                                                     | **never echoes apiKey**               |
| `LogIngestRequest`     | `serviceId (@NotNull UUID)`, `timestamp (@NotNull Instant)`, `level (@NotNull LogLevel)`, `logger`, `thread`, `message (@NotBlank)` | the appender contract                 |
| `LogQueryRequest`      | `serviceId`, `level`, `from`, `to`, `keyword`, `page (default0)`, `size (default20)`                                                | compact constructor defaults          |
| `LogQueryResponse`     | `logs (List<LogDocument>)`, `totalHits`, `page`                                                                                     |                                       |
| `HealthStatusResponse` | `serviceId`, `status`, `lastCheckedAt`, `responseTimeMs`                                                                            |                                       |
| `MetricQueryResponse`  | `serviceId`, `metricName`, `points (List<Point(timestamp, value)>)`                                                                 |                                       |

---

## 3. Database & hypertable (Step 2)

`spring.jpa.hibernate.ddl-auto=update` creates the plain tables on first boot. The TimescaleDB-specific conversion is **manual, one-time**:

```sql
CREATE EXTENSION IF NOT EXISTS timescaledb;
SELECT create_hypertable('metric_points', 'timestamp');
```

Run via: `docker exec -it watchlog-postgres psql -U root -d watchlog_db`

> Because `MetricPoint` was refactored to the composite PK including `timestamp` (Error Fix #7), `create_hypertable` succeeds — TimescaleDB rejects unique indexes that don't include the partition column.

---

## 4. Repositories (Step 3)

- **JPA:** `ProjectRepository` (`findByProjectName`, `existsByProjectName`), `ServicesRepository` (`findByProject_ProjectId`, `existsByServiceNameAndProject_ProjectId`), `HealthCheckResultRepository` (`findFirstByService_ServiceIdOrderByTimestampDesc`), `MetricPointRepository` (`findByServiceIdAndMetricNameAndTimestampBetween...`, plus a **native** `findBucketed` using `time_bucket(:bucket, timestamp)` → `Object[]{bucket, avg(value)}`).
- **ES:** `LogDocumentRepository extends ElasticsearchRepository<LogDocument, String>` with derived queries (`findByServiceIdAndLevelAndTimestampBetween...`, `findByMessageContaining...`).

---

## 5. Kafka wiring (Step 4) — `config/KafkaConfig`

Everything is configured **in code** so `application.properties` only sets the bootstrap server:
- Topic `log-events` (1 partition) defined as a `NewTopic` — auto-created on boot.
- Producer factory `StringSerializer` key + `JsonSerializer<LogEventMessage>` value → `KafkaTemplate`.
- Consumer factory: group `log-indexer`, `earliest`, `StringDeserializer` + `JsonDeserializer(LogEventMessage.class, mapper, false)` → `ConcurrentKafkaListenerContainerFactory` for `@KafkaListener`.
- **Both serializers use a dedicated `kafkaObjectMapper()`** that registers `JavaTimeModule` and disables `WRITE_DATES_AS_TIMESTAMPS` (ISO-8601) — spring-kafka's own bare mapper cannot handle `Instant` (Error Fix #10).

---

## 6–7. Ingest → Index (Steps 5–6)

**`LogController`** `POST /api/logs` → **`LogIngestionService.ingest`**:
1. Load `Services` by `serviceId` → **404** if unknown.
2. Resolve `projectId` server-side from `service.getProject()`.
3. Build `LogEventMessage` (UUID→String, `level.name()`, `timestamp` from request).
4. `kafkaTemplate.send("log-events", key=serviceId, event)` → returns **202 Accepted** (async; errors logged in the callback).

**`LogIndexerService`** `@KafkaListener(topics="log-events", groupId="log-indexer")`:
- Maps `LogEventMessage → LogDocument` via `toDocument()` and saves to ES. No try/catch on purpose — an ES failure surfaces and Kafka retries rather than silently dropping the event.

**Flow:** `POST /api/logs → Kafka log-events → LogIndexerService → Elasticsearch application-logs`. Kafka is the decoupling buffer between ingestion and search.

---

## 8. Log query (Step 7) — `LogQueryService`

`GET /api/logs` builds a dynamic `CriteriaQuery` (via `ElasticsearchOperations`) from whichever filters are present:
- `service_id` term, `level` term, `timestamp` range (from inclusive / to exclusive), `message` full-text `match` (keyword).
- Sorts `timestamp DESC`, paginates `PageRequest(page, size)` (defaults0/20).
- **Criteria uses the ES document field names** (`service_id`, not `serviceId`) — matching the `@Field(name=...)` mapping.

---

## 9. Health poller (Step 8) — `HealthCheckService`

`@Scheduled(fixedDelay = 30_000, initialDelay = 10_000)` iterates all services, calls `baseUrl + healthCheckEndpoint` via a `RestClient` with **3s connect/read timeouts** (a hanging endpoint can't block the scheduler). 2xx → `UP`, any exception/other → `DOWN`. Saves a `HealthCheckResult` per probe. `currentStatus()` returns the latest result per service (`UNKNOWN` if never checked) as `HealthStatusResponse`. Exposed by `GET /api/health`.

---

## 10. Metrics (Step 9) — `MetricPointPoller` + `MetricQueryService`

- `MetricPointPoller` `@Scheduled(fixedDelay = 15_000)` writes synthetic `cpu_usage` + `memory_usage_mb` rows per service (values are placeholders — swap for a real Micrometer/Prometheus scrape).
- `MetricQueryService` defaults `from` = last hour, `bucket` = `1 minute`; runs `MetricPointRepository.findBucketed` (TimescaleDB `time_bucket()`), maps each `{bucket, avg(value)}` row → `MetricQueryResponse.Point`. Exposed by `GET /api/metrics?serviceId=&metricName=&from=&to=&bucket=`.
- **Requires the hypertable** (Section 3).

---

## 11. WebSocket live streaming (Step 10)

**Deferred** — frontend comes later. The `spring-boot-starter-websocket` dependency is already in the pom.

---

## 12. REST layer (Step 11)

- **`ProjectService`/`ProjectController`**: `POST /api/projects` (201, **409** on duplicate name), `GET /api/projects`, `GET /api/projects/{id}` (404).
- **`ServicesService`/`ServicesController`**: `POST /api/services` (201, **404** if project missing, **409** on duplicate name-in-project), `GET /api/services?projectId=` (optional filter), `GET /api/services/{id}` (404).
- Status codes: `201` create, `400` validation, `404` missing, `409` conflict.

---

## 13. Appender library + upi integration (Step 12)

### The library — `D:\Project2\watchlog\watchlog-appender\` (separate Maven project, Java 17)
Zero transitive deps (only `logback-classic` + `slf4j-api` as `provided`):

| File | What it does |
|---|---|
| `LogEventPayload` | public `record(UUID serviceId, Instant timestamp, String level, String logger, String thread, String message)` — mirrors the ingest contract |
| `WatchlogHttpAppender` | `UnsynchronizedAppenderBase<ILoggingEvent>` — **async**: bounded queue (10k) + single daemon sender thread; drains on `stop()`; recursion guard (never re-forwards its own package's logs); optional `X-API-Key` header; JDK `HttpClient` with 2s timeouts; send failures only log a warning (never throw into the logging path) |
| `JsonWriter` | hand-rolled minimal JSON serializer — correct escaping, `null` for absent logger/thread/message, **no trailing comma** |

Config via `logback-spring.xml`:
```xml
<appender name="WATCHLOG" class="com.datalog.watchlog.appender.WatchlogHttpAppender">
  <url>${WATCHLOG_URL:-http://localhost:7000}</url>
  <serviceId>${WATCHLOG_SERVICE_ID}</serviceId>
</appender>
```

### Integration into `upi-wallet-system` (auth-service pilot)
- pom dep `com.datalog:watchlog-appender:0.1.0`
- `logback-spring.xml` with the WATCHLOG appender on `<root>` (console output preserved)
- **Docker packaging**: the `maven:3.9` build containers can't see the local `.m2`, so the jar is **vendored into each build context** (`lib/watchlog-appender-0.1.0.jar`) and installed into the builder with `mvn install:install-file` before `mvn dependency:go-offline`.
- Compose env: `WATCHLOG_URL: http://host.docker.internal:7000` (containers reach the host-run watchlog; `host.docker.internal` is Docker Desktop's host alias — `localhost` inside a container would be the container itself) and `WATCHLOG_SERVICE_ID` = the registered service UUID.

**Registered services:** `upi-wallet-system` project `23b1b31e-9c61-4f9e-a25a-e4802d174c88`; `auth-service` service `3305bdb9-b351-4772-b918-044f08e95759`.

---

## 14. 🐛 Error Fix Log (complete)

| # | Symptom | Root cause | Fix |
|---|---|---|---|
| 1 | ES `id` didn't map to `_id` | `LogDocument` used JPA's `@Id` | → `org.springframework.data.annotation.Id` |
| 2 | Compile error | DTOs imported `com.datalog.watchlog.enums.*` (nonexistent) | → `model.enums` |
| 3 | Type mismatch | DTOs used `Long` IDs; models use `UUID`/`String` | aligned all DTO IDs to `UUID`/`String` |
| 4 | Insert failed | `Services.apiKey` nullable=false but optional in request | `@PrePersist` auto-generates UUID |
| 5 | Duplicates possible | no unique constraints | `unique` on `project_name`; `(project_id, service_name)` unique |
| 6 | Dead import | `jdk.jfr.Timespan` unused | removed |
| 7 | `create_hypertable` failed | surrogate PK didn't include partition column | `@IdClass` composite PK `(service_id, metric_name, timestamp)` |
| 8 | Compile error `TypeReference` not found | spring-kafka's Jackson is optional | added `jackson-databind` |
| 9 | Startup `400 [es/indices.exists]` | Spring Data ES 6.1 targets ES 9.4.2, compose ran 8.13 | bumped compose to `elasticsearch:9.4.2` |
| 10 | Kafka `Instant` serialization `InvalidDefinitionException` | spring-kafka `JsonSerializer` builds a bare mapper without jsr310 | added `jackson-datatype-jsr310` + dedicated `kafkaObjectMapper()` (JavaTimeModule, ISO-8601) used by both serializer + deserializer; **full rebuild required** after pom change |
| 11 | `steps.md` SQL typo | `metric_point` vs `metric_points` | corrected |
| 12 | Docker build failed resolving `watchlog-appender` | build container's `.m2` is empty; artifact not on Central | vendor jar into context + `mvn install:install-file` in Dockerfile |

---

## 15. Final API surface

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/projects` | create project (201 / 409) |
| GET | `/api/projects`, `/api/projects/{id}` | list / get (404) |
| POST | `/api/services` | create service (201 / 404 / 409) |
| GET | `/api/services` (`?projectId=`), `/api/services/{id}` | list / get (404) |
| POST | `/api/logs` | ingest log → Kafka → ES (202) |
| GET | `/api/logs` | search (serviceId/level/from/to/keyword/page/size) |
| GET | `/api/health` | per-service status (UP/DOWN/UNKNOWN) |
| GET | `/api/metrics` | bucketed time-series (`time_bucket`) |

Testing curls for every endpoint: **`apistest.md`**.

---

## 16. How to run & verify

1. **Infra:** `docker compose up -d` from `dockcompos/` (Postgres 5444, ES 9200, Kafka 9000).
2. **Backend:** run `WatchlogApplication` (port **7000**). On first boot JPA creates the tables; then run the hypertable SQL (Section 3) once.
3. **Register:** `POST /api/projects` then `POST /api/services` (get the service UUID).
4. **Ingest + view:** `POST /api/logs` (expect 202) → after ~1s `GET /api/logs?serviceId=<uuid>` or query ES `localhost:9200/application-logs/_search`.
5. **Health/metrics:** `GET /api/health` (30s cadence), `GET /api/metrics?serviceId=<uuid>&metricName=cpu_usage` (15s cadence).
6. **Stream from a real app:** build+`mvn install` `watchlog-appender`, wire into a service (see Section 13).

**Environment gotchas:** Maven is installed at `D:\maven\apache-maven-3.9.16` (not on PATH — use the full path or add it); the watchlog backend runs on the host while its infra is containerized, so containers reach it via `host.docker.internal:7000`.

