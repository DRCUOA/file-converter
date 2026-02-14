# Dev Guardrails — Java File Converter

Master document mapping all [devg.md](../devg.md) guardrails to this Java/JavaFX build.

---

## 0. Mindset Rule (Non-Negotiable)

Before any feature work:

- Architecture is defined → [architecture.md](architecture.md)
- Invariants are written → [invariants.md](invariants.md)
- Guardrails are automated → CI, ArchUnit, Checkstyle, SpotBugs
- CI fails loud → No silent passes; violations block merge

---

## 1. Define System Boundaries

**Doc:** [architecture.md](architecture.md)

- System layers, dependency direction, external interfaces, data ownership
- Rules: UI cannot import CloudConvert SDK; Core cannot import JavaFX; no circular deps

---

## 2. Define Core Invariants

**Doc:** [invariants.md](invariants.md)

**Location:** `tests/invariants/`

- Data integrity, ownership rules, validation, security, referential integrity
- Tests exist before feature impl; they may fail initially — that is correct

---

## 3. Install Tooling (Java Stack)

| devg (Node/TS) | Java equivalent |
|----------------|------------------|
| Vitest/Jest | JUnit 5 |
| Supertest | MockWebServer / WireMock (API) |
| Stryker | PIT (mutation testing) |
| dependency-cruiser | ArchUnit |
| ESLint | Checkstyle, SpotBugs, PMD |
| Husky | Maven/Gradle plugins (enforcer, failsafe) |

**Required:** JUnit 5, ArchUnit, Checkstyle, SpotBugs, PIT (mutation), Gradle/Maven

---

## 4. Configure Linting as Guardrail

**Files:** `checkstyle.xml`, SpotBugs config, PMD rules

| Rule | Purpose |
|------|---------|
| No raw types | Prevent `any`-like shortcuts |
| No unused vars/imports | Clean code |
| No `System.out` (use SLF4J) | Structured logging |
| No empty catch blocks | Fail loud |
| Cyclomatic complexity ≤ 10 | Maintainability |
| Max nesting depth 3 | Readability |
| Max lines per method 50 | Single responsibility |

**Optional:** `no-warning-comments` — no TODOs survive in committed code.

---

## 5. Enforce Architecture Boundaries

**Tool:** ArchUnit

**File:** `src/test/java/architecture/ArchitectureTest.java`

Example rules:

```java
@AnalyzeClasses(packages = "app")
class ArchitectureTest {
    @ArchTest
    static final ArchRule ui_does_not_import_cloudconvert =
        noClasses().that().resideInAPackage("..ui..")
            .should().dependOnClassesThat().resideInAPackage("..cloudconvert..");

    @ArchTest
    static final ArchRule core_does_not_import_javafx =
        noClasses().that().resideInAPackage("..core..")
            .should().dependOnClassesThat().resideInAPackage("javafx..");
}
```

CI: `./gradlew test` runs ArchUnit; violations fail build.

---

## 6. Define Testing Layers

**Scaffold before features:**

```
tests/
  unit/
  integration/
  invariants/
  security/
  performance/
```

Even if empty initially. Forces mental separation.

---

## 7. Security Baseline Tests

**File:** `tests/security/SecurityBaselineTest.java`

Include (adapted for desktop app):

- API key not in logs or stack traces
- Path traversal blocked (output path cannot escape chosen dir)
- Invalid/malformed settings do not crash app
- CloudConvert 401/403 handled (no credential leak in UI)

---

## 8. Query / Call Count Instrumentation

**Context:** No database; CloudConvert API calls are the "queries."

Wrap `CloudConvertFacade` with call counter in test mode:

```java
// Test: batch of 3 files must not exceed 3 jobs (no redundant API calls)
test("batch of 3 files does not exceed expected API call budget", () -> {
    resetCallCounter();
    batchRunner.run(batchOf3);
    assertThat(getCloudConvertCallCount()).isLessThanOrEqual(EXPECTED_BUDGET);
});
```

Prevent N+1 or redundant polling before it exists.

---

## 9. Performance Budget Template

**File:** `tests/performance/PerformanceBudgetTest.java`

```java
@Test
void validationCompletesUnder50ms() {
    long start = System.nanoTime();
    Validation.validate(batchItem);
    long durationMs = (System.nanoTime() - start) / 1_000_000;
    assertThat(durationMs).isLessThan(50);
}
```

Define budgets early. Do not wait until performance is "bad."

---

## 10. Memory Leak Template

**File:** `tests/performance/MemoryLeakTest.java`

```java
@Test
void noMemoryGrowthOverRepeatedCalls() {
    long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    for (int i = 0; i < 50; i++) {
        runOperation();
    }
    System.gc();
    long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    assertThat(after - before).isLessThan(5_000_000); // 5MB
}
```

Signals: resource safety matters (streams, connections closed).

---

## 11. Mutation Testing Early

**Tool:** PIT (Pitest)

**Config:** `build.gradle` / `pom.xml`

Goal: > 80% mutation score before expanding codebase. Prevents false confidence.

```bash
./gradlew pitest
```

---

## 12. CI Before Feature Work

**File:** `.github/workflows/ci.yml`

Minimal pipeline:

1. Checkout
2. Set up Java 21
3. `./gradlew check` (compile, lint, tests)
4. ArchUnit (in test phase)
5. Mutation testing (optional nightly)

Every PR must pass. No exceptions.

---

## 13. Branch Discipline

- Each change atomic
- Each atomic change adds or modifies tests
- No multi-feature branches
- One invariant per issue if possible

Prevents change 789 from touching 49 and 140 silently.

---

## 14. Regression Ratchet

After first stable milestone:

- Snapshot API contracts (CloudConvert request/response shapes)
- Snapshot output naming rules
- Snapshot performance numbers

Use AssertJ `satisfies` or JSON snapshot for contracts. Snapshots freeze contracts; they are not laziness.

---

## 15. Feature Flags (Optional)

All new features behind flags. Reduces blast radius. For MVP, may defer.

---

## 16. Fail Loud Rule

- No silent fallback: no `|| []` / `Optional.orElse(empty)` unless justified
- No try/catch that swallows errors
- All unexpected states throw

```java
@Test
void invalidStateThrows() {
    assertThatThrownBy(() -> invalidCall()).isInstanceOf(IllegalStateException.class);
}
```

Protects against AI smoothing over logic gaps.

---

## 17. Commit Hooks

**Tool:** Gradle `com.diffplug.spotless` or Maven `maven-enforcer-plugin`

Pre-commit (via `./gradlew check` in Git hook):

- Lint must pass
- Tests must pass
- Optional: block if mutation score drops

Build friction intentionally.

---

## 18. Documentation Before Expansion

Before adding feature group:

- Update [architecture.md](architecture.md)
- Update [invariants.md](invariants.md)
- Update performance budget doc

Do not expand blind.

---

## 19. Checklist Before Coding

See [checklist.md](checklist.md).
