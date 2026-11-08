# Background draft preparation

Resume tailoring runs from a durable PostgreSQL queue. The normal Mac launcher starts the worker with the backend; no extra installation, subscription or Kafka service is needed. Both frontend and backend must be restarted after pulling this change. Flyway adds V9 without changing historical migrations or original resumes.

## Try the workflow

1. Sign in, upload a resume, select it as primary, and discover or paste a job.
2. Open Matches and choose **Generate truthful tailored draft**. The HTTP request queues work rather than waiting for model execution.
3. In **Draft preparation**, watch **Queued** → **Preparing local AI draft** → **Ready for review**. Navigate away or refresh; the task remains available when returning to Matches. Results are never inserted into an editor automatically.
4. Choose **Open draft task**, review/edit the content, explicitly approve it if appropriate, and save a new version. Word and TXT export work as before. Source snapshots reflect the resume used when queued, even after later edits or further version saves.
5. For cancellation, choose **Cancel draft task**. Queued tasks cancel immediately. Running tasks show **Cancellation requested** until the model returns or the lease expires; their output is discarded. This does not forcibly stop the external Ollama process.
6. If the model is unavailable or its response is invalid, the task shows a controlled error. Correct the local model/service problem and choose **Retry draft for job**. This creates a separate task using the current primary resume and job description; the failed record is retained. No silent model retries occur.

Tasks and results are private to their owner. The history shows the latest 20 tasks. A running task prevents another active task for that user, and the queue accepts at most 20 active tasks globally. One scheduled worker and a database-serialized running slot limit model work to one task at a time during normal operation. The old preview API uses the queue too.

## Restart and limits

Queued tasks remain in PostgreSQL and are picked up after backend restart. A running task abandoned by a crashed backend becomes **Failed** after its five-minute lease expires; it is not claimed successful or automatically replayed. Cancellation requests abandoned by a crash become **Cancelled** on expiry. Old workers cannot overwrite a task already marked terminal. Recovery is checked by the worker's polling loop; it is not instantaneous on restart.

The local model client uses bounded connection/read timeouts. Cancellation is cooperative. This local implementation does not claim exactly-once model execution during arbitrary distributed failures, immediate model interruption, job-feed background task recovery, full history pagination, or automatic snapshot retention/deletion. Running multiple independently configured inference clients outside JobPulse is outside its concurrency controls.

## API compatibility

- `POST /api/tailoring/tasks/{jobId}` queues a task and returns **202** with task metadata.
- The former `POST /api/tailoring/{jobId}` also returns **202 task metadata**, rather than a synchronous draft. Older clients must be updated.
- `GET /api/tailoring/tasks` returns the owner's latest metadata without draft bodies.
- `GET /api/tailoring/tasks/{id}` returns an owned task and its draft when successful.
- `POST /api/tailoring/tasks/{id}/cancel` cancels/request-cancels owned work; terminal results remain unchanged.
- Document saves may reference a completed `taskId` or an owned `sourceDocumentId`; the server resolves the source snapshot and validates matching resume/job identifiers. Clients cannot supply an arbitrary original snapshot.

Normal document ownership and review requirements still apply. No task submits an application or enables automatic application submission.

## Verification

Run `mvn verify`, frontend `npm test` / `npm run build`, and `bash scripts/run-full-stack-e2e.sh` with Docker available and its test ports free. Database tests disable the scheduled worker and explicitly exercise its state transitions; the browser runner uses the real scheduled worker and an explicitly mocked Ollama protocol server. See [verification evidence](VERIFICATION.md) for counts and limits.
