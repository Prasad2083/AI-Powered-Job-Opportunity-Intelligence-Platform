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
nohup mvn spring-boot:run > .local-logs/backend.log 2>&1 < /dev/null &
echo "$!" > .local-logs/backend.pid
(
  cd frontend
  npm install
  nohup npm run dev -- --host 127.0.0.1 > ../.local-logs/frontend.log 2>&1 < /dev/null &
  echo "$!" > ../.local-logs/frontend.pid
)
echo "Waiting for the browser interface..."
for i in $(seq 1 90); do
  if curl -fsS http://127.0.0.1:5173/ >/dev/null; then
    if ! kill -0 "$(cat .local-logs/backend.pid)" 2>/dev/null; then
      echo "Backend stopped unexpectedly; check .local-logs/backend.log" >&2
      exit 1
    fi
    open http://localhost:5173
    echo "JobPulse opened: http://localhost:5173"
    echo "Backend logs: .local-logs/backend.log"
    exit 0
  fi
  sleep 2
done
echo "Frontend did not become ready. Check .local-logs/frontend.log and .local-logs/backend.log" >&2
exit 1
