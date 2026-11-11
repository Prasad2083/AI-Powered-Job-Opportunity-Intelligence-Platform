# Verify and publish the preserved local commits

Use this fallback only while the managed execution environment cannot run Docker/network/browser checks or publish source. It does not waive release gates. The last confirmed remote development head is `8d295ed`; local work is preserved on `feature/production-workflow-20261027`.

A Git bundle is prepared in the managed workspace at `/workspace/shared/downloads/JobPulse-pending-commits.bundle`. Commit-named copies are retained alongside it; use `git bundle list-heads /path/to/bundle` to identify the included branch head. It contains unpublished commits and requires the existing `8d295ed` history; it is not a clone of the complete repository, model weights or a database export. Transfer it privately to an existing checkout when workspace file download is available. If it cannot be downloaded, leave the local commits intact and restore execution access instead; no successful transfer is assumed.

## Import without overwriting local work

On the Mac, first inspect `git status --short`. Preserve existing edits before proceeding. Do not delete untracked files, reset the branch or force-push to make these commands work.

```bash
git switch feature/production-workflow-20261027
git pull --ff-only
git bundle verify /path/to/JobPulse-pending-commits.bundle
git fetch /path/to/JobPulse-pending-commits.bundle feature/production-workflow-20261027
git merge --ff-only FETCH_HEAD
```

If a prerequisite is missing, a merge is not a fast-forward or an existing file blocks the operation, stop and retain the error and working changes for review. Do not discard work to bypass it.

## Validate before pushing

Use the existing Java 21 JDK, Maven, Node and Docker Desktop installation. No model download is needed for mocked integration tests. Frontend dependencies can be reused if the lockfile has not changed; install them only if missing or out of date.

```bash
mvn -B -ntp verify
mvn -B -ntp dependency:list -DoutputFile=target/security-dependencies.txt
python3 scripts/audit-maven.py
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix frontend audit --audit-level=moderate
```

Stop the normal frontend/backend before `bash scripts/run-full-stack-e2e.sh`: ports 3000, 8080 and 11435 must be free. The runner uses a separate ephemeral database and an explicitly mocked Ollama protocol server; preserve the normal PostgreSQL volume. If Chromium is missing, install the project's Playwright Chromium browser before running it.

Only after the checks pass:

```bash
git push origin feature/production-workflow-20261027
```

Review PR #1 and the new GitHub Actions results. Do not merge, deploy or enable real submissions. Report test failures as failures, rather than marking the new schema or archived-listing workflow verified based only on unit tests.

## Remaining acceptance

The isolated runner does not validate the real model on the Mac. Native startup, real resume extraction/tailoring quality, historical credential rotation and the other gates in [verification](VERIFICATION.md), [security](../SECURITY.md) and [release status](../PRODUCTION_STATUS.md) still apply. Confirm actual results before declaring the November 15 release ready.
