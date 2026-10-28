# JobPulse on macOS (local-first, no paid subscription)

## Install once
- Homebrew (https://brew.sh)
- Java 21: `brew install openjdk@21`
- Maven: `brew install maven`
- Node.js: `brew install node`
- Docker Desktop or another compatible local Docker runtime (Docker Desktop licensing varies by organization; alternatives include Colima).
- Ollama: `brew install ollama` or download from https://ollama.com.

## Download the AI model
In Terminal:
```bash
ollama serve
```
Open a second Terminal:
```bash
ollama pull qwen3:4b
```
Keep Ollama running while tailoring resumes.

## Local environment
Copy `.env.example` to `.env`, **do not commit** it. Set:
```env
DB_PASSWORD=choose-a-unique-local-password
JWT_SECRET=generate-at-least-64-random-bytes-for-this-local-instance
```
Generate JWT_SECRET via `openssl rand -hex 64`.
Use only local settings; never expose PostgreSQL or Ollama ports to the public internet.

## Run database
```bash
docker compose --env-file .env -f docker-compose.local.yml up -d
```

## Start backend
```bash
export DB_USERNAME=jobpulse DB_NAME=jobpulse DB_HOST=localhost DB_PORT=5432
export DB_PASSWORD="$(grep '^DB_PASSWORD=' .env | cut -d= -f2-)"
export JWT_SECRET="$(grep '^JWT_SECRET=' .env | cut -d= -f2-)"
mvn spring-boot:run
```
Backend URL: http://localhost:8080/api.

## Start React frontend
```bash
cd frontend
npm install
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```
Open http://localhost:5173.

## Jobs
Greenhouse and Lever discovery requires known public board/site slugs. Set `JOBPULSE_FEEDS_GREENHOUSE_BOARDS` and `JOBPULSE_FEEDS_LEVER_SITES` before starting backend. If none are configured and mock mode is disabled, ingestion returns zero real jobs; it does **not** pretend to have fetched jobs. Mock mode can be turned on with `JOBPULSE_FEEDS_INCLUDE_MOCK=true` for demonstrations.

## Scope
Resume upload, ranking, and Ollama-generated drafts are implemented but require end-to-end local verification. External applications are not automatically submitted; providers must support authorized submission and confirmation. The application cannot truthfully claim to have applied to a job based only on displaying a link.
