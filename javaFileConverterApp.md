# Java File Converter App — Developer Guide

## Project Goal

Build a **native desktop batch file converter** that uses the CloudConvert API to convert user-selected files and save results locally. This is an **MVP** plan.

**MVP scope:** Ship with three conversions — **MOD→MOV**, **JPEG→WEBP**, **DOCX→PDF**.

**Extensibility requirement:** The design must make it straightforward to add further conversions later (e.g. new profiles, formats, options) without major refactoring.

Deliver a single-window JavaFX GUI with multi-file selection, configurable profiles, controlled concurrency, per-file error isolation, and deterministic local saving. Target: packaged macOS `.app` via `jpackage`.

---

## Architecture Overview

### What you're building

A **single-window JavaFX app** that:

* lets you **multi-select files**
* assigns each file a **profile** (MOD→MOV, JPEG→WEBP, DOCX→PDF)
* runs **one CloudConvert job per file** (upload → convert → export/url → download → save)
* shows **per-file progress + logs**
* saves **only successful outputs** to the chosen folder

### Modules

| Module | Contents |
| ------ | -------- |
| `ui/` | JavaFX screens, view models |
| `domain/` | `BatchJob`, `BatchItem`, `ConversionProfile`, `BatchItemStatus` |
| `cloudconvert/` | `CloudConvertClientFactory`, `CloudConvertFacade`, `PipelineRunner` |
| `persistence/` | Profiles + recent output dir in local config (JSON) |

### Project layout

```
app/
  ui/
    MainApp.java
    MainController.java
    model/
      BatchItemFx.java
      ConversionProfileFx.java
  core/
    BatchRunner.java
    PipelineWorker.java
    CloudConvertFacade.java
    Profiles.java
    Validation.java
    OutputNaming.java
    RetryPolicy.java
  persistence/
    AppSettings.java
    SettingsStore.java
```

FX model classes use JavaFX properties; core classes are plain Java.

### Stack

* **Java 21+**, **JavaFX 21+**
* **CloudConvert Java SDK** (`com.cloudconvert:cloudconvert-java`) [4]
* **SLF4J + Logback** (structured logs; mirror into GUI)
* **Jackson** (persist profiles + last output folder)
* **jpackage** for macOS `.app`

---

## Approach & Design

### Pipeline per file

For each file, run:

1. `import/upload` — upload the file
2. `convert` — apply selected conversion options
3. `export/url` — get temporary download URL(s)
4. Download + save locally (only if successful)

CloudConvert uses jobs + tasks; you can create tasks via operation endpoints. [1] One job per file gives:

* clean failure isolation
* deterministic input → output mapping
* simpler cancellation and retry
* straightforward progress reporting

Do **not** combine files into one job. For a desktop batch converter, per-file isolation wins. (Multi-input jobs are only useful for merge/zip workflows.)

### Why JavaFX

* File/directory pickers
* Background workers (`Task`, `Service`) for responsive UI
* UI binding (progress bars, tables, logs)
* Straightforward `jpackage` packaging

---

## UI Structure

### Top bar

* Profile dropdown: `MOD → MOV`, `JPEG → WEBP`, `DOCX → PDF`
* Output directory picker
* Concurrency slider/spinner (default 2)

### Center: TableView

| File | Type | Profile | Status | Progress | Output path | Message |
| ---- | ---- | ------- | ------ | -------- | ----------- | ------- |
| … | detected | … | Queued/Uploading/Converting/Exporting/Downloading/Saving/Done/Failed/Skipped/Canceled | 0–100% | … | last error / note |

### Right panel: Profile options

* **MOD→MOV:** resolution dropdown, CRF slider (optional)
* **JPEG→WEBP:** quality slider, lossless toggle
* **DOCX→PDF:** no options initially

### Bottom: Log viewer

* Append-only TextArea, levels INFO/WARN/ERROR
* "Save Log…" button
* Per-file details (raw CloudConvert task message on failure)

### Batch controls

* Add Files… | Remove Selected | Clear | Start | Cancel (batch) | Cancel Selected
* "Stop after N errors" toggle
* "Skip ineligible files" toggle

---

## Core Data Model

### ConversionProfile

```java
public record ConversionProfile(
  String id,                 // "mod-mov", "jpg-webp", "docx-pdf"
  String displayName,
  String inputFormat,        // "mod" / "jpg" / "docx"
  String outputFormat,       // "mov" / "webp" / "pdf"
  Map<String, Object> convertOptions
) {}
```

### BatchItem

```java
public final class BatchItem {
  public final Path input;
  public final Path outputDir;
  public final ConversionProfile profile;

  public volatile String status;
  public volatile double progress;     // 0..1
  public volatile String message;
  public volatile Path outputPath;

  public volatile String jobId;
  public volatile String uploadTaskId;
  public volatile String exportTaskId;

  public BatchItem(Path input, Path outputDir, ConversionProfile profile) { ... }
}
```

### FX wrapper

`BatchItemFx` mirrors fields as `StringProperty`, `DoubleProperty`, etc., and holds a reference to `BatchItem`.

---

## CloudConvert Integration

### Primitives

| Step | Task | Notes |
| ---- | ---- | ----- |
| Upload | `import/upload` | API returns form-like upload target [2]; SDK has upload helper [4] |
| Convert | `convert` | Options depend on input/output format; use Job Builder [3] |
| Export | `export/url` | Temporary URLs, 24h validity; tasks deleted after [5] |

### Errors & rate limiting

* `422` — validation failures
* `429` — rate limiting (honour `Retry-After`)
* `5xx` / `503` — transient; retry with backoff

### Avoid sync endpoint for video

`sync.api.cloudconvert.com` is **not** recommended for long-running jobs (e.g. MOD→MOV) due to timeouts. [1] Use **polling** in your worker thread instead of a single blocking HTTP request.

### CloudConvertFacade

Thin wrapper around the SDK. Create client from env/properties/string; upload via `importUsing().upload(...)`; download via `files().download(url)`. [4]

```java
public interface CloudConvertFacade {
  String createJobForFile(String uploadTaskName, String convertTaskName, String exportTaskName,
                          ConversionProfile profile) throws Exception;

  TaskResponse createUploadTaskAndUpload(Path file) throws Exception;

  JobResponse getJob(String jobId) throws Exception;

  TaskResponse getTask(String taskId) throws Exception;

  InputStream download(String url) throws Exception;

  void cancelTask(String taskId) throws Exception;  // optional
  void cancelJob(String jobId) throws Exception;    // optional
}
```

### Job creation strategy

**Recommended:** Upload first, then create job referencing the upload task. You immediately have a valid input task id; job creation stays deterministic.

Task names: `import-<uuid>`, `convert-<uuid>`, `export-<uuid>`. API uses `"tasks": { "name": {...}, ... }` with tasks referencing each other by name. [1]

### Polling loop

* Poll every 2–5 seconds
* Update status/progress
* Stop when job is `finished` or `error` [1]

---

## PipelineWorker

Runs in an `ExecutorService` thread — **never** on the FX thread.

### Stages

1. Validate local file vs profile
2. Create `import/upload` and upload bytes
3. Create job with convert + export/url referencing upload task
4. Poll job until finished/error
5. When export/url finished, get URL(s) [5]
6. Download to temp file, then atomic move to output folder
7. Mark success

### Cancellation

* Shared `AtomicBoolean cancelRequested`
* Check between stages and during polling sleep
* CloudConvert supports API-side task/job cancellation [6]

---

## BatchRunner

```java
ExecutorService pool = Executors.newFixedThreadPool(concurrency);
CompletionService<Void> cs = new ExecutorCompletionService<>(pool);
```

* Submit `PipelineWorker` per eligible file
* Track futures for per-item cancel
* On 429: use `Retry-After`; on 5xx/503: exponential backoff

**Defaults:** concurrency = 2; 1 retry for transient network; 0 for conversion failures.

---

## Conversion Profiles

**MVP:** Implement **3 predefined profiles** (MOD→MOV, JPEG→WEBP, DOCX→PDF). Do not start with a fully generic converter UI — fixed profiles keep validation and UX manageable.

**Extensibility:** Structure profiles so adding a new conversion is a matter of defining a new `ConversionProfile` (id, formats, options) and extending the curated format mapping in validation. The pipeline, UI, and persistence should accept new profiles without code changes.

### MOD → MOV

* `input_format=mod`, `output_format=mov`
* **Expose:** resolution (original/1080p/720p), video codec (default `h264`), crf (18–30, default `23`)
* **Do not expose:** frame rate, GOP, pixel format, bitrate

```json
{
  "operation": "convert",
  "input": "upload-task",
  "input_format": "mod",
  "output_format": "mov",
  "video_codec": "h264",
  "crf": 23
}
```

### JPEG → WEBP

* `input_format=jpg`, `output_format=webp`
* **Expose:** quality (0–100, default 85), lossless toggle (default OFF)
* **Do not expose:** chroma subsampling, ICC, metadata (add later)

```json
{
  "operation": "convert",
  "input": "upload-task",
  "input_format": "jpg",
  "output_format": "webp",
  "quality": 85
}
```

### DOCX → PDF

* `input_format=docx`, `output_format=pdf`
* **Engine:** `office` (not libreoffice unless needed)
* **Expose:** none initially — one-click simple
* **Optional later:** page orientation, password protect

```json
{
  "operation": "convert",
  "input": "upload-task",
  "input_format": "docx",
  "output_format": "pdf",
  "engine": "office"
}
```

---

## Validation (pre-flight)

Do not rely on CloudConvert errors as the primary validator. Pre-check locally:

1. **File exists, readable, size > 0**
2. **Format:** extension (primary), `Files.probeContentType()` (secondary)
3. **Profile compatibility:** curated mapping (mod→mov, jpeg→webp, docx→pdf)
4. **Output collision:** overwrite / rename / skip
5. **Parameter overrides:** apply naming templates if profile has placeholders

**Acceptable extensions:** `.mod`, `.jpg`/`.jpeg`, `.docx` — reject others before upload.

**File size:** if > 1GB, warn (do not hard fail). CloudConvert free plans have limits.

Optional: consult CloudConvert "list supported formats" API to confirm support. [3]

---

## Output Saving

CloudConvert returns **URLs**; you must download and write locally. [4]

**Best practice:**

* Download to `outputDir/.tmp/<name>.part`
* On success, move to `outputDir/<baseName>.<outputExt>` with `ATOMIC_MOVE`
* Filename rule: `originalBaseName + "." + outputFormat` (e.g. `holiday.mod` → `holiday.mov`)

**Multi-output:** subfolder per input, auto-zip, or `export/url` option `archive_multiple_files` for one ZIP URL. [5]

---

## Error Handling

Per file, capture: stage (UPLOAD/CONVERT/EXPORT/DOWNLOAD/SAVE), task status + message, local exception, retryability.

**Rules:**

* Upload/Convert/Export fails → mark failed; continue batch
* Download fails → retry once; if still fails, mark failed; continue batch
* Save fails (permissions/path) → mark failed; continue batch
* Mark "Succeeded" only after local write completes

---

## Logging

Each batch item stores: `timestamp`, `stage`, `cloudconvert_task_id`, `message`.

Example:

```
10:14:02 UPLOAD started
10:14:07 UPLOAD finished
10:14:08 CONVERT started
10:15:12 CONVERT finished
10:15:14 DOWNLOAD finished
```

---

## Critical Advice

These break most CloudConvert desktop apps:

* **Giant single batch job** — debugging hell
* **Manual format typing** — always use dropdowns
* **Too many advanced options early** — support nightmare
* **Direct download to output dir** — always temp file → atomic move
* **No retry logic** — add retry for HTTP 5xx, network timeout

---

## Guardrails

Before any feature work, install project hardening per [devg.md](devg.md). All guardrails are documented and mapped to this Java build in `docs/`:

| Doc | Purpose |
|-----|---------|
| [docs/architecture.md](docs/architecture.md) | System boundaries, dependency rules |
| [docs/invariants.md](docs/invariants.md) | Core invariants to test |
| [docs/guardrails.md](docs/guardrails.md) | All 19 guardrails mapped to Java stack |
| [docs/testing-strategy.md](docs/testing-strategy.md) | Test layers, security, performance, mutation |
| [docs/ci-and-discipline.md](docs/ci-and-discipline.md) | CI pipeline, branch discipline, commit hooks |
| [docs/checklist.md](docs/checklist.md) | Pre-coding checklist |

**Mindset:** Architecture defined, invariants written, guardrails automated, CI fails loud. See [docs/checklist.md](docs/checklist.md) before coding.

---

## Build Plan

0. Install guardrails: ArchUnit, Checkstyle, SpotBugs, PIT; scaffold `tests/`; CI pipeline
1. JavaFX UI skeleton + TableView + log panel
2. File picker + output dir picker + profile selection
3. Implement `CloudConvertFacade`: upload, create job, poll, export/url parse, download
4. Implement `PipelineWorker`
5. Implement `BatchRunner` + concurrency + cancel
6. Add dry-run validation (mark eligible/ineligible)
7. Persist settings (API key, last output dir, last profile)
8. Package with `jpackage`

---

## Packaging & Security

**jpackage:** Include Java runtime; produce native `.app` bundle.

**API key:** Store in app settings file or OS keychain (prefer keychain). Do not hardcode. SDK supports config via properties/env/system/string. [4]

---

## References

[1]: https://cloudconvert.com/api/v2/jobs "Jobs | CloudConvert API"
[2]: https://cloudconvert.com/api/v2/import "Import files | CloudConvert API"
[3]: https://cloudconvert.com/api/v2/convert "Convert Files API | CloudConvert"
[4]: https://github.com/cloudconvert/cloudconvert-java "CloudConvert Java SDK"
[5]: https://cloudconvert.com/api/v2/export "Export files | CloudConvert API"
[6]: https://cloudconvert.com/api/v2 "CloudConvert API"
[7]: https://cloudconvert.com/api/v2/quickstart "Quickstart Guide | CloudConvert API"
