# Product and hosting backlog

Recorded 2026-11-10 after the user's QAAnywhere review. These are planned increments, not completed features or additions to the current release promise. Preserve Java 21, Spring Boot, React/Vite, PostgreSQL and local Ollama. No new paid subscriptions, public deployment or real application submission is authorized by this backlog.

P0 = required before public hosting. P1 = next local product increments after current release blockers. P2 = optional follow-up. The [implementation checklist](IMPLEMENTATION_CHECKLIST.md) and [verification evidence](VERIFICATION.md) remain the source of acceptance status; this backlog does not replace them.

## Discovery and data quality

### JP-101 · P1 · Expand permitted job sources and employer coverage

Extend the existing employer configuration first, prioritizing actual Canadian QE, SDET, Java backend and AI-quality opportunities. Evaluate additional public APIs or feeds separately; a third-party aggregator's visible jobs are not automatically permission to republish its dataset.

Acceptance: each adapter documents source terms, attribution, identifiers, timestamps, pagination and rate limits; controlled-response tests cover schema changes, failures and complete-feed archival rules. Live availability checks identify when they ran and do not claim continued vacancy status. No mandatory paid feed. Keep manual descriptions private and unverified.

### JP-102 · P1 · Classify the role using software-testing context

Combine title and description evidence to distinguish software quality engineering from manufacturing, medical auditing and other meanings of QA. Continue supporting the user's Java/backend and AI-quality roles rather than narrowing JobPulse to a QA-only board.

Acceptance: a labelled fixture set includes software QA/SDET, backend Java, AI quality and unrelated QA examples; classification exposes reasons and uncertain cases for review. Job requirements are not inferred from a title alone. Preserve source text and allow preference filtering.

### JP-103 · P1 · Make remote eligibility explicit

Add work arrangement, stated eligible countries/regions, timezone restrictions and supporting source excerpts. Use explicit states such as Canada, US-only, worldwide and unknown. Distinguish job location from permission to work remotely from that location.

Acceptance: tests cover Canada-remote, US-only remote, Toronto/GTA hybrid, worldwide with exclusions, timezone restrictions and missing evidence. An unspecified region never becomes worldwide or verified eligibility. Work authorization remains user-declared/reviewed; no automatic eligibility or submission claim.

### JP-104 · P1 · Clarify posting age and freshness

Extend the existing publication/last-seen fields and job-details UI; do not replace working timestamp support. Show original publication date, first discovery and last successful check separately where available. Unknown publication dates remain unknown, and a provider's update time is not relabelled as publication time.

Acceptance: fixtures include old jobs discovered today, missing dates, changed listings and failed refreshes. Failed/deferred checks cannot make a listing look newly verified. Archived listings opened from saved links show their status and retain the user's associated documents/history.

### JP-105 · P1 · Deduplicate across sources

Extend existing provider-scoped deduplication using canonical employer application links and stable employer/job identifiers. Prefer the employer's original posting while retaining source provenance.

Acceptance: fixtures cover tracking parameters, meaningful URL parameters, reposts, genuinely separate jobs with similar titles, and changed descriptions. Avoid false merges. Private manual jobs must never be merged into or exposed through a public shared listing.

### JP-106 · P1 · Durable ingestion and visible refresh progress

Move long employer-feed refreshes out of HTTP requests into a PostgreSQL-backed workflow, reusing the local worker approach. Persistent retry metadata has a local implementation awaiting database verification; see the implementation checklist. A durable ingestion queue is still unimplemented.

Acceptance: bounded admission/concurrency, duplicate refresh prevention, persisted run reports, restart recovery and controlled retry behavior; queued/running/partial/failed/completed UI states survive refresh. Reports distinguish checked feeds from deferred/failed feeds and retain the existing no-archive-on-incomplete-feed rule. Tests verify existing user isolation. No Kafka requirement or additional hosted worker service.

## Optional product increments

### JP-201 · P2 · Personalized weekly shortlist

Start with an in-app shortlist using saved preferences, resume evidence, stated geographic restrictions and freshness. Include understandable reasons for ranking and unknown eligibility flags.

Acceptance: scores and recommendations use real saved source evidence, provide original application links and avoid duplicate recommendations. Do not report applications as submitted. Email delivery is a separate opt-in decision; no newsletter vendor, subscription or sharing of resumes is introduced here.

### JP-202 · P2 · Public discovery pages and portfolio presentation

If public hosting is approved later, consider crawlable pages for public employer listings and a separate portfolio showcase. User resumes, manual jobs, drafts, preferences and application history stay behind authorization.

Acceptance: guest/API/browser tests prove that public rendering, caches and page metadata cannot expose private records. Publish only approved public-source content and selected technical descriptions. No automatic publication of source code or internal deployment diagrams.

## Mandatory gates before public hosting

### JP-301 · P0 · Decide repository visibility and deployment artifacts

GitHub metadata confirmed the implementation repository was public on 2026-11-10. Hosting controls cannot make a public repository's code private. Decide whether to keep implementation private and publish a separate curated showcase, or intentionally retain selected public source. Repository visibility changes require the owner's explicit decision and are not performed by this backlog.

Acceptance: record the decision and access list; production artifacts exclude `.git`, environment files, logs, internal runbooks and unintended source archives. Historical credential rotation remains an independent release gate. Changing visibility cannot revoke copies already obtained.

### JP-302 · P0 · Enforce the client/server trust boundary

The browser receives frontend HTML/CSS/JavaScript and can inspect API URLs and responses. Minification or hiding API names is not an authorization boundary. Keep backend source, matching/tailoring execution, provider credentials, signing keys, database credentials and internal AI access on the server. Never place secrets in `VITE_*` variables or frontend bundles.

Acceptance: automated bundle/response checks use synthetic secret canaries; DTO/error/health responses exclude internal paths, secrets, stack traces and unnecessary private payloads. Preserve ownership tests for every private resource. Production source maps are omitted from public assets or kept in controlled private storage. No promise of making frontend code or API traffic invisible.

### JP-303 · P0 · Private infrastructure and an HTTPS entry point

Use a reviewed HTTPS gateway/reverse proxy for the approved frontend and API routes. Keep PostgreSQL, Ollama, worker internals and management interfaces inaccessible from the public network. The browser calls the authorized API; it never connects directly to the database or model service.

Acceptance: external network checks establish the intended exposed ports/routes and deny direct database/model/admin access, including container and IPv6 bindings. Verify TLS, allowed origins, restrictive security headers, authorization, rate limits and the chosen session/CSRF design. No provider or paid infrastructure is provisioned by this item.

### JP-304 · P0 · Restrict production diagnostics and document the exposure boundary

API documentation is currently enabled in application configuration and covered by the general authenticated-user rule. Add and test an explicit production policy: disable Swagger/OpenAPI by default or restrict required documentation to an approved administrative/internal path. Review management, health, error, version and logging exposure separately.

Acceptance: unauthenticated and ordinary-user tests cannot access production diagnostics or sensitive management endpoints; health exposes only approved operational status. Redact sensitive logs and verify that build artifacts do not publish internal documentation. These controls reduce unnecessary information disclosure; they do not replace secure APIs or guarantee that the underlying stack cannot be inferred.

## Evidence and limits of the QAAnywhere comparison

- [Creator's explanation](https://www.linkedin.com/posts/denis-bojkovski-107055ba_softwaretesting-qajobs-remoteqa-activity-7500066292369027072-xDIH): describes aggregation from roughly 15 sources, daily refresh, title filtering and original-posting links. Comments identify posting-age and geographic-eligibility concerns; those comments are feedback, not independently proven defects.
- [Editorial standards](https://qaanywhere.com/editorial): describes deduplication, source-derived labels, stale-listing removal and labelled summaries where full reproduction is unavailable.
- [Weekly page](https://qaanywhere.com/weekly/): a compact shortlist linked to detail/application pages; useful as a presentation pattern.
- [Privacy policy](https://qaanywhere.com/privacy): identifies its email provider. This does not identify its application host or backend implementation.

The review did not verify QAAnywhere's backend language, database, scheduler, hosting origin, hosting bill or private source code. JobPulse should adopt useful outcomes and trust boundaries without claiming to reproduce unverified internals.
