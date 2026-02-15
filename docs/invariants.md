# Core Invariants

**Guardrail 2:** Define core invariants before feature implementation.

These truths must hold for the system. Tests in `tests/invariants/` encode them.

---

## Data Integrity

| Invariant | Test location |
|-----------|---------------|
| Output file is written only after successful download | `tests/invariants/OutputIntegrityTest.java` |
| Temp file is always cleaned up on failure | `tests/invariants/TempFileCleanupTest.java` |
| Atomic move used for final save (no partial writes) | `tests/invariants/AtomicSaveTest.java` |
| Settings file is valid JSON; invalid file does not corrupt app state | `tests/invariants/SettingsIntegrityTest.java` |

---

## Ownership and Isolation

| Invariant | Test location |
|-----------|---------------|
| One CloudConvert job per file (no multi-file jobs) | `tests/invariants/JobIsolationTest.java` |
| Failure of one file does not affect others in batch | `tests/invariants/BatchIsolationTest.java` |
| PipelineWorker never runs on FX thread | `tests/invariants/FXThreadInvariantTest.java` |

---

## Validation

| Invariant | Test location |
|-----------|---------------|
| Only input formats represented by configured conversion profiles are accepted | `tests/invariants/FormatValidationTest.java` |
| File must exist, readable, size > 0 before upload | `tests/invariants/PreflightValidationTest.java` |
| Profile compatibility enforced before API call | `tests/invariants/ProfileCompatibilityTest.java` |

---

## Security

| Invariant | Test location |
|-----------|---------------|
| API key never hardcoded or logged | `tests/invariants/ApiKeySecurityTest.java` |
| Output path cannot escape chosen directory (path traversal) | `tests/invariants/PathTraversalTest.java` |

---

## Referential Integrity

| Invariant | Test location |
|-----------|---------------|
| BatchItem references valid ConversionProfile | `tests/invariants/ProfileReferenceTest.java` |
| Cancellation propagates to CloudConvert task/job when supported | `tests/invariants/CancelPropagationTest.java` |
