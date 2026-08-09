# Watchlog — Project Roadmap

Kafka-based log pipeline with TimescaleDB metrics integration. Stack: Spring Boot 4.1.0 (Java 21), TimescaleDB (Postgres 16), Elasticsearch 8.13, Kafka (KRaft single-node).

---

## ✅ Step 0 — Infrastructure (DONE)

Docker services up:

| Service | Port | Container |
|---|---|---|
| TimescaleDB (Postgres 16) | 5432 | `watchlog-postgres` |
| Elasticsearch 8.13 | 9200 | `watchlog-elasticsearch` |
| Kafka (KRaft) | 9092 | `watchlog-kafka` |

DB credentials: database `watchlog`, user `root`, password `root`.

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

Standalone `LogEventPayload` class + HTTP appender each microservice pulls in to POST logs to `/api/logs`. Deliberately NOT a shared dependency on the platform's DTO module.

---

## Step 13 (optional) — Extras

- Kafka UI container (`provectuslabs/kafka-ui`) for inspecting topics
- Frontend / query UI

---

### Design notes (from history)

- Entities never leak over the API — controllers map `Project`/`Service` entities to their Request/Response DTOs. Prevents leaking `apiKey` and coupling the DB schema to the public API shape.
- `LogEventPayload` in the appender library is standalone on purpose — microservices shouldn't depend on the platform's codebase just to log.
- If the app later gets containerized into the same compose file, add a second Kafka listener advertised as `watchlog-kafka:9092` for container-to-container traffic.
