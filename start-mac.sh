#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v brew >/dev/null || { echo "Install Homebrew first from https://brew.sh then rerun."; exit 1; }
echo "Installing free local dependencies (only missing ones)..."
brew list --formula openjdk@21 >/dev/null 2>&1 || brew install openjdk@21
for dep in maven node ollama; do
  brew list --formula "$dep" >/dev/null 2>&1 || brew install "$dep"
done
export PATH="$(brew --prefix openjdk@21)/bin:$(brew --prefix)/bin:$PATH"
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI was not found. Install and start Docker Desktop, then rerun." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker is installed but its engine is not running. Start Docker Desktop, then rerun." >&2
  exit 1
fi
if ! curl --silent --fail http://127.0.0.1:11434/api/tags >/dev/null; then
  nohup ollama serve > /tmp/jobpulse-ollama.log 2>&1 < /dev/null &
  for i in $(seq 1 30); do
    curl --silent --fail http://127.0.0.1:11434/api/tags >/dev/null && break
    sleep 1
  done
fi
echo "Downloading local AI model (first launch only)..."
ollama pull qwen3:4b
if [[ ! -f .env ]]; then
  umask 077
  printf 'DB_PASSWORD=%s\nJWT_SECRET=%s\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 64)" > .env
fi
set -a
source .env
set +a
docker compose --env-file .env -f docker-compose.local.yml up -d --wait
export DB_HOST=localhost DB_PORT=5432 DB_NAME=jobpulse DB_USERNAME=jobpulse
export JOBPULSE_OLLAMA_URL=http://localhost:11434
export VITE_API_BASE_URL=http://localhost:8080/api
mkdir -p .local-logs
echo "Starting backend and frontend..."
mvn -q clean -DskipTests package > .local-logs/build.log 2>&1 || { echo "Backend build failed; see .local-logs/build.log" >&2; exit 1; }
nohup mvn spring-boot:run > .local-logs/backend.log 2>&1 < /dev/null &
echo "$!" > .local-logs/backend.pid
(
  cd frontend
  npm install
  nohup npm run dev -- --host 127.0.0.1 > ../.local-logs/frontend.log 2>&1 < /dev/null &
  echo "$!" > ../.local-logs/frontend.pid
)
echo "Waiting for frontend and backend..."
for i in $(seq 1 90); do
  if ! kill -0 "$(cat .local-logs/backend.pid)" 2>/dev/null; then
    echo "Backend process stopped; see .local-logs/backend.log" >&2
    exit 1
  fi
  if curl -fsS http://127.0.0.1:3000/ >/dev/null 2>&1; then
    # Allow Spring Boot to finish starting, and test the public login endpoint.
    status="$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' --data '{}' http://127.0.0.1:8080/api/auth/login || true)"
    if [[ "$status" == "400" || "$status" == "422" ]]; then
      open http://localhost:3000
      echo "JobPulse opened: http://localhost:3000"
      echo "Backend logs: .local-logs/backend.log"
      exit 0
    fi
  fi
  sleep 2
done
echo "Startup not verified. Inspect .local-logs/backend.log and .local-logs/frontend.log" >&2
exit 1
