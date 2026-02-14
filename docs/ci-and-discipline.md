# CI and Branch Discipline

**Guardrails 12, 13, 17:** CI before feature work, branch discipline, commit hooks.

---

## CI Pipeline

**File:** `.github/workflows/ci.yml`

```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - run: ./gradlew check
      - run: ./gradlew pitest  # optional: nightly only to save time
```

**Check phase includes:**

1. Compile
2. Lint (Checkstyle, SpotBugs)
3. Unit tests
4. Integration tests
5. ArchUnit (architecture rules)
6. Mutation testing (optional, can be nightly)

Every PR must pass. No exceptions.

---

## Branch Discipline

| Rule | Rationale |
|------|-----------|
| Each change atomic | Easier review, revert, bisect |
| Each atomic change adds or modifies tests | No untested code |
| No multi-feature branches | Prevents silent cross-feature impact |
| One invariant per issue if possible | Clear traceability |

Prevents change 789 from touching 49 and 140 silently.

---

## Commit Hooks

**Option A:** Git hook calling `./gradlew check`

```bash
# .git/hooks/pre-commit
#!/bin/sh
./gradlew check || exit 1
```

**Option B:** Gradle `com.diffplug.spotless` + `com.github.spotbugs`; hook runs `./gradlew check`

**Enforced:**

- Lint must pass
- Tests must pass
- Optional: block if mutation score drops below threshold

Build friction intentionally. Prevents "I'll fix it later" commits.
