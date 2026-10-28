# Security findings and release constraints

Reviewed 2026-10-28. Current launcher is local-only; do not expose it publicly without additional review.

## Corrected

- Removed Spring Data REST to prevent repository endpoints bypassing controller ownership checks.
- Prevented password-hash JSON serialization and replaced application entity responses with explicit summaries.
- Scoped manual jobs, resumes, saved drafts, preferences and history to the authenticated user; unknown legacy manual ownership is not guessed.
- Invalid signatures/expired tokens return unauthorized; disabled users cannot log in; logout invalidates previous tokens through a persisted version. Default token lifetime is one hour.
- Bounded registration/upload input; original-file downloads require ownership; actual document parsers validate PDF/DOCX; invalid UTF-8/binary TXT is rejected.
- Stopped destructive dev schema recreation and automatic fabricated sample-resume insertion.
- Removed successful mock-submission behavior. Manual completion is user-reported, not provider-confirmed.
- Replaced unconstrained model prose with validated original-line selection. Model output cannot add new professional facts to generated drafts. Users still must review edits and source accuracy.
- Patched the dependency set and bound the normal backend/launcher to loopback. Docker profile deliberately binds inside its container; do not expose its host ports publicly by default.

## Dependency evidence

The original resolved Maven scan had 36 affected packages. Spring Boot 3.5.16 plus supported patch overrides for Tomcat, PostgreSQL JDBC, Jackson, Log4j API and Commons Lang, updated PDFBox/POI/Commons IO, and removal of unused WebFlux reduced actionable findings to zero in the completed OSV scan. Frontend npm audit reports zero after Vite/test-tool upgrades.

Two advisories still affect the selected Spring MVC version but require features this REST application does not use:

- `GHSA-j9f9-w8pj-32f8`: SSE fragment rendering.
- `GHSA-pc63-qcmh-9cmg`: XSLT view rendering with implicit wildcard view names.

They are **not patched or erased from reports**. [Version-specific reviews](security/advisory-reviews.json) document non-applicability, expire on 2026-11-28 and fail when relevant source patterns appear. Review again when upgrading Spring or adding any view/streaming support. `scripts/audit-maven.py` fails on new/unreviewed advisories or scanner failure. This is applicability triage, not a claim that every dependency has zero published advisories.

## Remaining gates

Repository history contains formerly tracked environment/log files. Removing them from the current tree does not revoke previously exposed credentials. Rotate any real credentials that appeared there, assess who could access the history, and document that action before release. This session did not print or rotate secret values and did not rewrite history.

Browser tokens currently use localStorage. Any future XSS would expose them; public hosting should evaluate secure HttpOnly cookies, CSRF protection for that design, a restrictive CSP, TLS and dedicated origin policy. Public service also needs abuse throttling, password reset/account recovery, monitoring, backup/restore exercises and further authorization review. Rate-limit configuration from old planning documents is not a claim of distributed throttling implementation.

Resume content and original files reside in PostgreSQL. Use OS/disk encryption, private backups and appropriate database credentials; no application-level at-rest encryption or malware scanning is claimed. Image-only PDFs are not OCR-processed.

No paid service, billing, deployment or purchase was enabled.
