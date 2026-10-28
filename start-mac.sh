#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# Reuse the installed Java 21 JDK and Docker Desktop; never install software.
if [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  [[ -z "$JAVA_HOME" ]] || export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
elif command -v brew >/dev/null 2>&1 && [[ -d "$(brew --prefix)/opt/openjdk@21/bin" ]]; then
  export JAVA_HOME="$(brew --prefix)/opt/openjdk@21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
for dep in java javac mvn node npm ollama curl openssl docker lsof; do
  command -v "$dep" >/dev/null || { echo "Missing $dep. See MAC_LOCAL_SETUP.md; the launcher installs nothing." >&2; exit 1; }
done
java_version="$(java -version 2>&1 | head -n 1)"
[[ "$java_version" == *'"21.'* ]] || { echo "Java 21 JDK required (found $java_version)." >&2; exit 1; }
node -e 'const [major,minor]=process.versions.node.split(".").map(Number); if(major<22 || (major===22 && minor<16)) process.exit(1)' || { echo "Node 22.16+ required." >&2; exit 1; }
mkdir -p .local-logs
if ! mkdir .local-logs/launcher.lock 2>/dev/null; then
  echo "Another launch is in progress. If a previous launcher crashed, remove .local-logs/launcher.lock after verifying it stopped." >&2
  exit 1
fi
started_backend=""; started_frontend=""; launch_ok=false
cleanup() {
  rmdir .local-logs/launcher.lock 2>/dev/null || true
  if [[ "$launch_ok" != true ]]; then
    [[ -z "$started_backend" ]] || kill "$started_backend" 2>/dev/null || true
    [[ -z "$started_frontend" ]] || kill "$started_frontend" 2>/dev/null || true
  fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM
running() { [[ -f ".local-logs/$1.pid" ]] && kill -0 "$(cat ".local-logs/$1.pid")" 2>/dev/null; }
for service in backend frontend; do
  port=8080; [[ "$service" != frontend ]] || port=3000
  if ! running "$service" && lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "Port $port is occupied by an untracked process. Stop it before launching JobPulse." >&2
    exit 1
  fi
done
if [[ ! -f .env ]]; then
  umask 077
  printf 'DB_PASSWORD=%s\nJWT_SECRET=%s\n' "$(openssl rand -hex 24)" "$(openssl rand -hex 64)" > .env
fi
set -a
source .env
set +a
: "${DB_PASSWORD:?Set DB_PASSWORD in .env}"
: "${JWT_SECRET:?Set JWT_SECRET in .env}"
[[ ${#JWT_SECRET} -ge 64 && "$JWT_SECRET" != replace-* && "$JWT_SECRET" != your-* ]] || { echo "Generate JWT_SECRET with openssl rand -hex 64." >&2; exit 1; }
if ! docker info >/dev/null 2>&1; then
  echo "Start your existing Docker Desktop installation, then rerun." >&2; exit 1
fi
docker compose --env-file .env -f docker-compose.local.yml up -d --wait --wait-timeout 90
export OLLAMA_HOST=http://127.0.0.1:11434
export JOBPULSE_OLLAMA_URL=http://127.0.0.1:11434
export JOBPULSE_OLLAMA_MODEL="${JOBPULSE_OLLAMA_MODEL:-qwen3:4b}"
if ! curl --max-time 3 --silent --fail "$JOBPULSE_OLLAMA_URL/api/tags" >/dev/null; then
  nohup ollama serve > .local-logs/ollama.log 2>&1 < /dev/null &
  for ((i=0;i<30;i++)); do
    curl --max-time 3 --silent --fail "$JOBPULSE_OLLAMA_URL/api/tags" >/dev/null && break
    sleep 1
  done
fi
curl --max-time 3 --silent --fail "$JOBPULSE_OLLAMA_URL/api/tags" >/dev/null || { echo "Ollama did not become ready; see .local-logs/ollama.log" >&2; exit 1; }
if ! ollama show "$JOBPULSE_OLLAMA_MODEL" >/dev/null 2>&1; then
  echo "Downloading missing local model: $JOBPULSE_OLLAMA_MODEL"
  ollama pull "$JOBPULSE_OLLAMA_MODEL"
fi
export JOBPULSE_FEEDS_GREENHOUSE_BOARDS="${JOBPULSE_FEEDS_GREENHOUSE_BOARDS-canonical,reddit,gitlab}"
export JOBPULSE_FEEDS_LEVER_SITES="${JOBPULSE_FEEDS_LEVER_SITES-ataccama,caseware}"
export DB_HOST=localhost DB_PORT=5432 DB_NAME=jobpulse DB_USERNAME=jobpulse
export SERVER_PORT=8080 SERVER_ADDRESS=127.0.0.1
# Use Vite's same-origin proxy, preserving the existing local URL.
export VITE_API_BASE_URL=/api
if ! running backend; then
  echo "Building backend..."
  mvn -q -DskipTests package > .local-logs/build.log 2>&1 || { echo "Backend build failed; see .local-logs/build.log" >&2; exit 1; }
  nohup java -jar target/job-pulse-auto-apply-1.0.0.jar > .local-logs/backend.log 2>&1 < /dev/null &
  started_backend=$!; echo "$started_backend" > .local-logs/backend.pid
fi
if ! running frontend; then
  if [[ ! -d frontend/node_modules || ! -f .local-logs/frontend-lock.json ]] || ! cmp -s frontend/package-lock.json .local-logs/frontend-lock.json; then
    (cd frontend && npm ci)
    cp frontend/package-lock.json .local-logs/frontend-lock.json
  fi
  (cd frontend && exec nohup node node_modules/vite/bin/vite.js --host 127.0.0.1) > .local-logs/frontend.log 2>&1 < /dev/null &
  started_frontend=$!; echo "$started_frontend" > .local-logs/frontend.pid
fi
echo "Waiting for frontend and backend..."
for ((i=0;i<90;i++)); do
  running backend || { echo "Backend stopped; see .local-logs/backend.log" >&2; exit 1; }
  running frontend || { echo "Frontend stopped; see .local-logs/frontend.log" >&2; exit 1; }
  if curl --max-time 3 -fsS http://127.0.0.1:3000/ >/dev/null 2>&1 && curl --max-time 3 -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
    launch_ok=true
    echo "JobPulse ready: http://localhost:3000"
    echo "Logs: .local-logs/backend.log and .local-logs/frontend.log"
    if command -v open >/dev/null 2>&1; then open http://localhost:3000 || true; fi
    exit 0
  fi
  sleep 2
done
echo "Startup not verified. Inspect .local-logs/backend.log and .local-logs/frontend.log" >&2
exit 1
