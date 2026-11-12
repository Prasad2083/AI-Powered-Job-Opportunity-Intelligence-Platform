#!/usr/bin/env bash
# Isolated PostgreSQL + real Spring Boot + React; only Ollama is a protocol fixture.
set -euo pipefail
cd "$(dirname "$0")/.."
for port in 8080 3000 11435; do
  if curl --max-time 1 -s "http://127.0.0.1:$port/" >/dev/null 2>&1; then
    echo "Port $port is in use; stop the service before running isolated E2E tests." >&2; exit 1
  fi
done
mkdir -p .local-logs/e2e
container="jobpulse-e2e-$$"; backend_pid=""; fixture_pid=""
cleanup() {
  [[ -z "$backend_pid" ]] || kill "$backend_pid" 2>/dev/null || true
  [[ -z "$fixture_pid" ]] || kill "$fixture_pid" 2>/dev/null || true
  docker rm -f "$container" >/dev/null 2>&1 || true
}
trap cleanup EXIT
trap 'exit 130' INT TERM
export POSTGRES_PASSWORD="$(openssl rand -hex 24)"
docker run -d --name "$container" -e POSTGRES_PASSWORD -e POSTGRES_USER=jobpulse -e POSTGRES_DB=jobpulse -p 127.0.0.1::5432 postgres:16-alpine >/dev/null
export DB_PORT="$(docker inspect -f '{{(index (index .NetworkSettings.Ports "5432/tcp") 0).HostPort}}' "$container")"
export DB_HOST=127.0.0.1 DB_NAME=jobpulse DB_USERNAME=jobpulse DB_PASSWORD="$POSTGRES_PASSWORD"
export JWT_SECRET="$(openssl rand -hex 64)"
export JOBPULSE_FEEDS_GREENHOUSE_BOARDS= JOBPULSE_FEEDS_LEVER_SITES= JOBPULSE_FEEDS_INCLUDE_MOCK=false
export JOBPULSE_OLLAMA_URL=http://127.0.0.1:11435 JOBPULSE_OLLAMA_MODEL=qwen3:4b
export SERVER_PORT=8080 SERVER_ADDRESS=127.0.0.1
export SPRING_PROFILES_ACTIVE=default VITE_API_BASE_URL=/api
for ((i=0;i<30;i++)); do
  docker exec "$container" pg_isready -U jobpulse -d jobpulse >/dev/null 2>&1 && break
  sleep 1
done
"${MAVEN_BIN:-mvn}" -B -ntp -DskipTests package > .local-logs/e2e/build.log 2>&1
python3 scripts/mock-ollama.py 11435 > .local-logs/e2e/ollama-fixture.log 2>&1 & fixture_pid=$!
java -jar target/job-pulse-auto-apply-1.0.0.jar > .local-logs/e2e/backend.log 2>&1 & backend_pid=$!
ready=false
for ((i=0;i<60;i++)); do
  kill -0 "$backend_pid" 2>/dev/null || { echo "Backend failed; inspect .local-logs/e2e/backend.log" >&2; exit 1; }
  if curl --max-time 2 -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
[[ "$ready" == true ]] || { echo "Backend readiness timed out" >&2; exit 1; }
JOBPULSE_LIVE_API=true npm --prefix frontend run test:e2e
