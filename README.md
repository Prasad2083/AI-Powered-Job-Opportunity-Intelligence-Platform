# JobPulse

Local job discovery, evidence-based resume matching, and reviewed resume drafts. Continues the existing Java 21 / Spring Boot / React / PostgreSQL project on `feature/production-workflow-20261027` and PR #1.

**Local development release candidate; PR remains draft. No public deployment or automatic application submission.**

## Run on your Mac

Start your existing Docker Desktop installation. Install Java 21 **JDK**, Maven, Node 22.16+ (or Node 24), and Ollama once if missing. Then:

```bash
git switch feature/production-workflow-20261027
bash start-mac.sh
```

Open **http://localhost:3000**. Backend: http://localhost:8080/api. The launcher installs no software, reuses PostgreSQL 16 and existing Ollama models, generates local secrets if `.env` is absent, and checks both services before reporting success. It downloads `qwen3:4b` only if that model is missing. Existing database volumes are retained.

If `.env` already exists, use a unique `DB_PASSWORD` and generate `JWT_SECRET` with `openssl rand -hex 64`. Changing a PostgreSQL password environment variable does not change credentials inside an existing database volume. Never delete the volume to fix a login problem.

See [macOS setup and recovery](MAC_LOCAL_SETUP.md).

## Workflow

1. Register (password 12–72 characters, maximum 72 UTF-8 bytes) and log in.
2. Upload a PDF, DOCX, or UTF-8 TXT resume. Preview/edit extracted text; the uploaded original remains downloadable and unchanged. Uploading a replacement retains older versions. Select which resume to use for matching.
3. Choose **Find jobs**, or paste a private job description. Defaults include real Greenhouse boards `canonical,reddit,gitlab` and Lever sites `ataccama,caseware`. Listings may change or close; manual entries and mocks are explicitly unverified.
4. Set target roles and locations in **Profile & preferences**. Review matching scores and their evidence. Scores are ranking aids, not hiring probabilities or verified eligibility.
5. Generate a tailored draft using local Ollama. The model selects original evidence lines; the server copies those lines verbatim and retains the full original. Edit supported wording, review every claim, and save a separate version. Download TXT documents.
6. Complete applications on official employer sites. Explicitly confirm completion to record a **user-reported** application; this is never provider confirmation. Track interview, offer, rejection, or withdrawal statuses in history.
7. Logout revokes all existing tokens for your account. Sessions otherwise expire after one hour by default.

No paid AI API, cloud account, subscription, or billing is required. See [provider research](docs/APPLICATION_PROVIDER_RESEARCH.md) for why unattended submission remains disabled.

## Verify

```bash
mvn -B -ntp clean verify
mvn -B -ntp dependency:list -DoutputFile=target/security-dependencies.txt
python3 scripts/audit-maven.py
cd frontend
npm ci
npm test
npm run build
npm audit --audit-level=moderate
npx playwright install chromium
cd ..
bash scripts/run-full-stack-e2e.sh
```

Docker must be running for PostgreSQL tests. The full-stack runner creates and removes an isolated database; only Ollama is mocked. Ports 3000, 8080, and 11435 must be free. It never uses your normal JobPulse database.

- [Implementation checklist and remaining work](docs/IMPLEMENTATION_CHECKLIST.md)
- [Executed verification and limitations](docs/VERIFICATION.md)
- [Security findings](SECURITY.md)
- [Current release status](PRODUCTION_STATUS.md)
- [Future cloud deployment](docs/CLOUD_READINESS.md)

Historical phase documents describe earlier plans and completion claims; the documents linked above are authoritative for this branch.
