# Testing Strategy

**Guardrails 6–11:** Testing layers, security baseline, instrumentation, performance, memory, mutation.

---

## Test Layers

| Layer | Location | Purpose |
|-------|----------|---------|
| Unit | `tests/unit/` | Single-class logic, Validation, OutputNaming, RetryPolicy |
| Integration | `tests/integration/` | CloudConvertFacade with MockWebServer; PipelineWorker flow |
| Invariants | `tests/invariants/` | Architectural truths (see [invariants.md](invariants.md)) |
| Security | `tests/security/` | API key handling, path traversal, credential leak |
| Performance | `tests/performance/` | Budgets, memory growth |

---

## Security Baseline Tests

**File:** `tests/security/SecurityBaselineTest.java`

| Test | Assertion |
|------|-----------|
| `apiKeyNotInLogs` | No API key substring in log output |
| `pathTraversalBlocked` | Output path cannot resolve outside chosen dir |
| `invalidSettingsNoCrash` | Malformed JSON settings → graceful fallback, no NPE |
| `authFailureNoCredentialLeak` | 401/403 from CloudConvert → generic message in UI |

---

## API Call Budget

**Context:** No database; CloudConvert calls are the "queries."

| Operation | Budget |
|-----------|--------|
| Single file pipeline | 1 upload + 1 job create + N polls + 1 download |
| Batch of K files | K × (upload + job + polls + download); no redundant calls |
| Validation (pre-flight) | 0 CloudConvert calls |

Instrument `CloudConvertFacade` in test; assert budgets in integration tests.

---

## Performance Budgets

| Operation | Budget |
|-----------|--------|
| `Validation.validate(BatchItem)` | < 50 ms |
| `OutputNaming.resolve(Path, Profile)` | < 5 ms |
| Settings load/save | < 100 ms |

Define early. Do not wait until performance is "bad."

---

## Memory Leak Template

```java
@Test
void noMemoryGrowthOverRepeatedValidation() {
    long before = measureHeapUsed();
    for (int i = 0; i < 50; i++) {
        Validation.validate(validBatchItem);
    }
    System.gc();
    Thread.sleep(100);
    long after = measureHeapUsed();
    assertThat(after - before).isLessThan(5_000_000); // 5MB
}
```

Signals: streams, connections, temp files closed.

---

## Mutation Testing

**Tool:** PIT (Pitest)

**Goal:** > 80% mutation score on core logic before expanding.

**Target packages:** `app.core`, `app.persistence`

Run: `./gradlew pitest`

Mutation testing prevents false confidence from tests that pass but do not assert meaningfully.
