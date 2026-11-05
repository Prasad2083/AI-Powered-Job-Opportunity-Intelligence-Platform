# Application interfaces and consent

Reviewed 2026-10-28. Public job listing access does not establish authorization to submit applications.

| Provider | Listings | Application interface | JobPulse behavior |
| --- | --- | --- | --- |
| Greenhouse | Public Job Board GET endpoints | Job Board application POST requires employer-managed authentication and board-specific required fields. Candidate login to JobPulse supplies neither credential nor employer authorization. | Public discovery and official application links. No POST adapter enabled. |
| Lever | Public Postings GET API, paginated with `limit`/`skip` | Postings API documents application POSTs, integration API keys, required fields and rate limits. Integration setup/authorization and reliable receipts must be established per provider/employer. | Public discovery and official links. No unauthorized submission or credential assumptions. |
| Manual | User-pasted description | No provider verification or submission capability implied. | Private matching aid, explicitly unverified. |

References: [Greenhouse Job Board API](https://developers.greenhouse.io/job-board.html), [Lever Postings API](https://github.com/lever/postings-api).

Automatic submission remains globally unavailable (HTTP 501), including mock clients. No browser automation bypass, CAPTCHA bypass, paid integration, or fake receipt has been added.

Manual completion requires an explicit user confirmation. Records use `USER_REPORTED_*` statuses and `providerConfirmed=false`; they never claim verified provider delivery. The user may update their reported interview/offer/rejection/withdrawal status. A transaction locks the user's record and rejects duplicate applications for that job.

Before implementing a real adapter: obtain provider/employer authorization and credentials; discover required questions; implement user-defined eligibility rules and per-application approval; persist an immutable approved payload, idempotency key and provider response; reconcile timeouts before retrying; mark submitted only on a verifiable receipt. No eligibility configuration currently enables real submissions.
