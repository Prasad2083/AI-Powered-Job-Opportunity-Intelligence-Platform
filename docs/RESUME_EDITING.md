# Resume editing safeguards

The Resumes page now aborts a superseded preview request and ignores its late success/error. When previews are selected quickly, only the latest selection can open an editor. Leaving the page aborts an unfinished preview.

Once an editor is open, save or cancel before opening another resume, uploading a replacement or changing the primary selection. This prevents those in-page actions from replacing unsaved text. During a save the text area, Cancel and competing mutations are disabled. A failed save retains the draft, re-enables editing and allows explicit retry; success closes the editor after saving the captured text to the selected resume ID.

Original download remains available and unchanged. Uploading a replacement still retains previous originals. Backend ownership, original-file storage and migrations are unchanged in this increment.

## Validate locally

1. With two resumes, select previews quickly; the last selection should open. Save or cancel before selecting another.
2. Edit text and save. The editor locks while saving; reopen to verify persisted extracted text.
3. Download the original and compare it with the uploaded file. Editing extracted text must not change original bytes.
4. A failed save must show an error and retain the unsaved text for retry. Automated browser fixtures cover this safely without requiring a real service outage.

Limits: this protects in-page request/editor races, not cross-tab optimistic concurrency, offline persistence or unsaved navigation/browser-close protection. Finish saving before leaving the page. Same-account simultaneous editors can still overwrite extracted text; immutable original uploads remain retained. CI/browser and native Mac acceptance are recorded separately in [verification](VERIFICATION.md), not assumed from local component tests.
