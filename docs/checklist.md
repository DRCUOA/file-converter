# Checklist Before Coding

**Guardrail 19:** Only then write feature code.

---

## Pre-Feature Checklist

- [ ] Architecture documented → [architecture.md](architecture.md)
- [ ] Dependency rules enforced → ArchUnit in CI
- [ ] Invariant tests written → [invariants.md](invariants.md)
- [ ] Security baseline tests exist → `tests/security/`
- [ ] Performance budget defined → [testing-strategy.md](testing-strategy.md)
- [ ] API call counter integrated (for CloudConvert) → instrumentation in tests
- [ ] Mutation testing configured → PIT in `build.gradle`
- [ ] CI pipeline active → `.github/workflows/ci.yml`
- [ ] Pre-commit hooks installed → `./gradlew check` on commit

---

## Before Adding a New Feature Group

- [ ] Update [architecture.md](architecture.md) if new modules/layers
- [ ] Update [invariants.md](invariants.md) if new invariants
- [ ] Update performance budget doc if new hot paths
- [ ] Do not expand blind

---

## Reference

Full guardrails: [guardrails.md](guardrails.md)
