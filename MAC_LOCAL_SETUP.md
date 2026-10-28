# JobPulse on macOS

## Prerequisites

Reuse Docker Desktop and its local images. Start Docker Desktop before launching. Verify `docker info` succeeds. No extra database GUI, Redis, cloud hosting, or paid AI subscription is needed.

Install missing tools once using your preferred method: Java 21 JDK (`java` and `javac`), Maven, Node 22.16+ or 24, and Ollama. Homebrew is optional. Examples: `brew install openjdk@21 maven node ollama`. The launcher itself installs nothing.

## Launch

```bash
git switch feature/production-workflow-20261027
bash start-mac.sh
```

The launcher selects an installed Java 21 JDK, reuses Docker Desktop, starts only the `postgres:16-alpine` service, and retains the named volume. On first launch it creates a private `.env` with random database and HS512 JWT secrets. An existing `.env` is never overwritten.

Ollama runs locally at `127.0.0.1:11434`. `qwen3:4b` is downloaded only when `ollama show` confirms it is absent. Later launches reuse it. Model downloads require internet and local disk space; tailoring subsequently runs locally.

Frontend: **http://localhost:3000**. Backend health: http://127.0.0.1:8080/api/health. PostgreSQL, backend, frontend, and Ollama remain bound to loopback for this launcher. Vite proxies `/api` to the backend.

Logs and process IDs live in `.local-logs/`. Success requires both tracked processes to remain alive and both HTTP readiness checks to pass. A failed launch stops processes it started; it retains the database and existing services. Repeated launches reuse tracked running processes and installed frontend dependencies when the lockfile is unchanged.

## Configuration

If copying `.env.example`, replace its placeholders before running. Generate a JWT secret using `openssl rand -hex 64`; do not commit `.env`. Keep its file permissions private: `chmod 600 .env`.

Optional settings in `.env`:

```dotenv
JOBPULSE_OLLAMA_MODEL=qwen3:4b
JOBPULSE_FEEDS_GREENHOUSE_BOARDS=canonical,reddit,gitlab
JOBPULSE_FEEDS_LEVER_SITES=ataccama,caseware
JOBPULSE_FEEDS_INCLUDE_MOCK=false
```

Empty feed lists disable those providers. Employer slugs are configurable; a listing returned today is not a guarantee of future availability. The launcher uses local ports 5432, 8080, 3000, and 11434.

Do not retain an old `SPRING_PROFILES_ACTIVE=dev` entry unless needed. Both normal and dev configurations now use Flyway and `ddl-auto=validate`; neither creates sample resumes or drops tables.

## Recovery and stopping

- **Missing Java compiler:** install a Java 21 JDK, not only a JRE. Verify `javac -version`.
- **Docker unavailable:** start your existing Docker Desktop. Do not install another Docker engine.
- **Database authentication failure:** existing volumes retain their original credentials. Restore the matching local password or deliberately rotate it inside PostgreSQL after taking a backup. Do not run `docker compose down -v`.
- **Build/startup failure:** inspect `.local-logs/build.log`, `backend.log`, and `frontend.log`. The launcher will not report readiness for a stopped backend.
- **Port occupied:** stop the conflicting process. The launcher refuses to take over an untracked service.
- **Stale launcher lock:** verify the earlier launcher stopped before removing `.local-logs/launcher.lock`.
- **Ollama unavailable:** inspect `.local-logs/ollama.log`, `ollama list`, and `ollama show qwen3:4b`. Draft generation returns a useful error; it never silently substitutes fabricated content.
- **Historical original download missing:** earlier versions stored only extracted text. Upload the original again; bytes that were never stored cannot be recovered.

To stop, inspect the PID files and their process commands, then terminate those JobPulse processes. Stop the database without deleting its volume:

```bash
docker compose --env-file .env -f docker-compose.local.yml stop
```

Back up before upgrades with `pg_dump` inside your database container and store the result securely. Flyway V1–V3 are unchanged; new migrations are additive. Actual Docker Desktop / Apple Silicon / Ollama performance still requires verification on your Mac; Linux test results do not establish macOS compatibility.
