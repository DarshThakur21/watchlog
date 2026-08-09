#!/usr/bin/env bash
#
# test_watchlog.sh — smoke test for the Watchlog API
#
# Walks the documented flow: Project -> Service -> Logs -> Health -> Metrics,
# exercising both the success paths and the documented error cases.
#
# Usage:
#   ./test_watchlog.sh                      # uses http://localhost:7000, creates a fresh project+service
#   ./test_watchlog.sh http://host:port      # custom base URL
#
# To test against a project/service you already created (e.g. via Postman)
# instead of creating new ones, export these before running:
#   export EXISTING_PROJECT_ID="981640ed-1e45-46f1-b4a0-debcf5d212a8"
#   export EXISTING_SERVICE_ID="2ed6fe04-3770-47f2-b3a3-3ecde8281802"
#   ./test_watchlog.sh
# (This skips the create/duplicate/missing-field tests for projects and
# services, since those don't apply to already-existing resources, and runs
# the GET/logs/health/metrics tests against the IDs you provided.)
#
# Requires: curl, and (optionally) jq for JSON parsing (falls back to python3).

set -uo pipefail

BASE_URL="${1:-http://localhost:7000}"
PASS=0
FAIL=0

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

section() {
  echo
  echo -e "${BLUE}== $1 ==${NC}"
}

# Fires a request, stores status in HTTP_STATUS and body in BODY.
request() {
  local method="$1" url="$2" data="${3:-}"
  local resp
  if [ -n "$data" ]; then
    resp=$(curl -s -w '\n%{http_code}' -X "$method" "$url" \
      -H "Content-Type: application/json" -d "$data")
  else
    resp=$(curl -s -w '\n%{http_code}' -X "$method" "$url")
  fi
  HTTP_STATUS=$(printf '%s' "$resp" | tail -n1)
  BODY=$(printf '%s' "$resp" | sed '$d')
}

# Extracts a JSON field from $BODY (jq if available, else python3 fallback).
json_field() {
  local field="$1"
  if command -v jq >/dev/null 2>&1; then
    printf '%s' "$BODY" | jq -r ".${field} // empty" 2>/dev/null
  else
    printf '%s' "$BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    v = d.get('${field}', '')
    print(v if v is not None else '')
except Exception:
    print('')
" 2>/dev/null
  fi
}

# Compares HTTP_STATUS against expected and prints PASS/FAIL.
check() {
  local desc="$1" expected="$2"
  if [ "$HTTP_STATUS" == "$expected" ]; then
    echo -e "  ${GREEN}PASS${NC}  $desc (status $HTTP_STATUS)"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}FAIL${NC}  $desc (expected $expected, got $HTTP_STATUS)"
    echo "        body: $BODY"
    FAIL=$((FAIL + 1))
  fi
}

now_iso() { date -u +%Y-%m-%dT%H:%M:%SZ; }

# ---------------------------------------------------------------------------
# preflight
# ---------------------------------------------------------------------------

section "Preflight"
echo "  Base URL: $BASE_URL"

if ! command -v jq >/dev/null 2>&1 && ! command -v python3 >/dev/null 2>&1; then
  echo -e "  ${YELLOW}Warning:${NC} neither jq nor python3 found — ID extraction will fail."
fi

request GET "$BASE_URL/api/projects"
if [ "$HTTP_STATUS" == "000" ] || [ -z "$HTTP_STATUS" ]; then
  echo -e "  ${RED}Cannot reach $BASE_URL — is the server running?${NC}"
  exit 1
fi
echo -e "  ${GREEN}Server reachable${NC} (status $HTTP_STATUS)"

# Unique names per run so reruns don't collide with leftover data.
STAMP=$(date +%s)
PROJECT_NAME="Demo-$STAMP"
SERVICE_NAME="demo-app-$STAMP"

# ===========================================================================
# 1. PROJECTS
# ===========================================================================
section "1. Projects — /api/projects"

if [ -n "${EXISTING_PROJECT_ID:-}" ]; then
  PROJECT_ID="$EXISTING_PROJECT_ID"
  echo "  Using existing projectId=$PROJECT_ID (skipping create/duplicate/missing-name tests)"
else
  # 1.1 create
  request POST "$BASE_URL/api/projects" \
    "{\"projectName\":\"$PROJECT_NAME\",\"projectDescription\":\"Demo project\"}"
  check "POST create project" 201
  PROJECT_ID=$(json_field id)
  echo "        -> projectId=$PROJECT_ID"

  # 1.2 duplicate name
  request POST "$BASE_URL/api/projects" \
    "{\"projectName\":\"$PROJECT_NAME\",\"projectDescription\":\"dup\"}"
  check "POST duplicate projectName -> 409" 409

  # 1.3 missing projectName
  request POST "$BASE_URL/api/projects" \
    '{"projectDescription":"no name"}'
  check "POST missing projectName -> 400" 400
fi

# 1.4 list all
request GET "$BASE_URL/api/projects"
check "GET all projects" 200

# 1.5 get one (valid)
if [ -n "$PROJECT_ID" ]; then
  request GET "$BASE_URL/api/projects/$PROJECT_ID"
  check "GET project by id" 200
else
  echo -e "  ${YELLOW}SKIP${NC}  GET project by id (no PROJECT_ID captured)"
fi

# 1.6 get one (unknown)
request GET "$BASE_URL/api/projects/00000000-0000-0000-0000-000000000000"
check "GET unknown project -> 404" 404

# ===========================================================================
# 2. SERVICES
# ===========================================================================
section "2. Services — /api/services"

if [ -n "${EXISTING_SERVICE_ID:-}" ]; then
  SERVICE_ID="$EXISTING_SERVICE_ID"
  echo "  Using existing serviceId=$SERVICE_ID (skipping create/duplicate/missing-field tests)"

  # 2.6 list all
  request GET "$BASE_URL/api/services"
  check "GET all services" 200

  # 2.7 list by project
  if [ -n "$PROJECT_ID" ]; then
    request GET "$BASE_URL/api/services?projectId=$PROJECT_ID"
    check "GET services by projectId" 200
  fi

  # 2.8 get one (valid)
  request GET "$BASE_URL/api/services/$SERVICE_ID"
  check "GET service by id" 200

  # 2.9 get one (unknown)
  request GET "$BASE_URL/api/services/00000000-0000-0000-0000-000000000000"
  check "GET unknown service -> 404" 404

elif [ -n "$PROJECT_ID" ]; then
  # 2.1 create
  request POST "$BASE_URL/api/services" \
    "{\"projectId\":\"$PROJECT_ID\",\"serviceName\":\"$SERVICE_NAME\",\"baseUrl\":\"http://localhost:8080\",\"healthCheckEndpoint\":\"/actuator/health\"}"
  check "POST create service" 201
  SERVICE_ID=$(json_field id)
  echo "        -> serviceId=$SERVICE_ID"

  API_KEY_FIELD=$(json_field apiKey)
  if [ -z "$API_KEY_FIELD" ]; then
    echo -e "  ${GREEN}PASS${NC}  apiKey correctly omitted from response"
    PASS=$((PASS + 1))
  else
    echo -e "  ${RED}FAIL${NC}  apiKey should not be returned, got: $API_KEY_FIELD"
    FAIL=$((FAIL + 1))
  fi

  # 2.2 missing projectId
  request POST "$BASE_URL/api/services" \
    "{\"serviceName\":\"orphan-service\"}"
  check "POST missing projectId -> 400" 400

  # 2.3 missing serviceName
  request POST "$BASE_URL/api/services" \
    "{\"projectId\":\"$PROJECT_ID\"}"
  check "POST missing serviceName -> 400" 400

  # 2.4 unknown projectId
  request POST "$BASE_URL/api/services" \
    '{"projectId":"00000000-0000-0000-0000-000000000000","serviceName":"ghost-service"}'
  check "POST unknown projectId -> 404" 404

  # 2.5 duplicate serviceName in same project
  request POST "$BASE_URL/api/services" \
    "{\"projectId\":\"$PROJECT_ID\",\"serviceName\":\"$SERVICE_NAME\",\"baseUrl\":\"http://localhost:8080\"}"
  check "POST duplicate serviceName in project -> 409" 409

  # 2.6 list all
  request GET "$BASE_URL/api/services"
  check "GET all services" 200

  # 2.7 list by project
  request GET "$BASE_URL/api/services?projectId=$PROJECT_ID"
  check "GET services by projectId" 200

  # 2.8 get one (valid)
  if [ -n "$SERVICE_ID" ]; then
    request GET "$BASE_URL/api/services/$SERVICE_ID"
    check "GET service by id" 200
  else
    echo -e "  ${YELLOW}SKIP${NC}  GET service by id (no SERVICE_ID captured)"
  fi

  # 2.9 get one (unknown)
  request GET "$BASE_URL/api/services/00000000-0000-0000-0000-000000000000"
  check "GET unknown service -> 404" 404
else
  echo -e "  ${YELLOW}SKIP${NC}  all service tests (no PROJECT_ID from step 1)"
fi

# ===========================================================================
# 3. LOGS
# ===========================================================================
section "3. Logs — /api/logs"

if [ -n "${SERVICE_ID:-}" ]; then
  # 3.1 ingest
  request POST "$BASE_URL/api/logs" \
    "{\"serviceId\":\"$SERVICE_ID\",\"timestamp\":\"$(now_iso)\",\"level\":\"INFO\",\"logger\":\"com.demo.App\",\"thread\":\"main\",\"message\":\"hello from test script\"}"
  check "POST ingest log -> 202" 202

  # 3.2 missing serviceId
  request POST "$BASE_URL/api/logs" \
    "{\"timestamp\":\"$(now_iso)\",\"level\":\"INFO\",\"message\":\"no service\"}"
  check "POST log missing serviceId -> 400" 400

  # 3.3 missing message
  request POST "$BASE_URL/api/logs" \
    "{\"serviceId\":\"$SERVICE_ID\",\"timestamp\":\"$(now_iso)\",\"level\":\"INFO\"}"
  check "POST log missing message -> 400" 400

  # 3.4 invalid level
  request POST "$BASE_URL/api/logs" \
    "{\"serviceId\":\"$SERVICE_ID\",\"timestamp\":\"$(now_iso)\",\"level\":\"BOGUS\",\"message\":\"bad level\"}"
  check "POST log invalid level -> 400" 400

  # 3.5 unknown serviceId
  request POST "$BASE_URL/api/logs" \
    "{\"serviceId\":\"00000000-0000-0000-0000-000000000000\",\"timestamp\":\"$(now_iso)\",\"level\":\"INFO\",\"message\":\"ghost\"}"
  check "POST log unknown serviceId -> 404" 404

  echo "  Waiting a few seconds for Kafka -> Elasticsearch indexing..."
  sleep 5

  # 3.6 search: default
  request GET "$BASE_URL/api/logs?size=5"
  check "GET logs (default)" 200

  # 3.7 search: by service + level
  request GET "$BASE_URL/api/logs?serviceId=$SERVICE_ID&level=INFO"
  check "GET logs by serviceId+level" 200

  # 3.8 search: keyword + time window + paging
  FROM="$(date -u -d '-1 hour' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-1H +%Y-%m-%dT%H:%M:%SZ)"
  TO="$(date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v+1H +%Y-%m-%dT%H:%M:%SZ)"
  request GET "$BASE_URL/api/logs?keyword=hello&from=$FROM&to=$TO&page=0&size=20"
  check "GET logs keyword+time window+paging" 200
else
  echo -e "  ${YELLOW}SKIP${NC}  all log tests (no SERVICE_ID from step 2)"
fi

# ===========================================================================
# 4. HEALTH
# ===========================================================================
section "4. Health — /api/health"

request GET "$BASE_URL/api/health"
check "GET health" 200
echo "  (note: entries show DOWN/UNKNOWN unless localhost:8080 is actually serving /actuator/health)"

# ===========================================================================
# 5. METRICS
# ===========================================================================
section "5. Metrics — /api/metrics"

if [ -n "${SERVICE_ID:-}" ]; then
  request GET "$BASE_URL/api/metrics?serviceId=$SERVICE_ID&metricName=cpu_usage"
  check "GET metrics (default window)" 200

  M_FROM="$(date -u -d '-1 hour' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-1H +%Y-%m-%dT%H:%M:%SZ)"
  M_TO="$(now_iso)"
  request GET "$BASE_URL/api/metrics?serviceId=$SERVICE_ID&metricName=cpu_usage&from=$M_FROM&to=$M_TO&bucket=5%20minutes"
  check "GET metrics (custom window+bucket)" 200
  echo "  (note: 'points' may be empty if the service was just registered — poller writes every 15s)"
else
  echo -e "  ${YELLOW}SKIP${NC}  all metrics tests (no SERVICE_ID from step 2)"
fi

# ===========================================================================
# summary
# ===========================================================================
section "Summary"
TOTAL=$((PASS + FAIL))
echo -e "  ${GREEN}Passed: $PASS${NC}   ${RED}Failed: $FAIL${NC}   Total: $TOTAL"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0