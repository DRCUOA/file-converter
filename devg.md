\033[1;35mPROJECT HARDENING SETUP GUIDE (DEV GUARDRAILS)\033[0m
\033[1;34m============================================================\033[0m

Pre-coding hardening setup to block regression, drift, and
AI-introduced shortcuts. Install before feature 001 exists.

\033[1;97mNAME\033[0m
\033[1;34m------------------------------------------------------------\033[0m

devg — Project hardening and dev guardrails for compound stability

\033[1;97mDESCRIPTION\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Build a ratcheted system where regression, drift, and AI shortcuts
are structurally blocked. Before any feature work: architecture is
defined, invariants are written, guardrails are automated, CI fails
loud. You are not "being slow" — you are building compound stability.

\033[1;34m============================================================\033[0m

\033[1;97m0. MINDSET RULE (NON-NEGOTIABLE)\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Before any feature work:
- Architecture is defined.
- Invariants are written.
- Guardrails are automated.
- CI fails loud.

\033[1;34m============================================================\033[0m

\033[1;97m1. DEFINE SYSTEM BOUNDARIES\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Create \033[1;91m/docs/architecture.md\033[0m

Document: system layers, dependency direction, external interfaces,
data ownership rules.

Example structure:

  \033[1;91mPresentation (Vue)\033[0m
  \033[1;91m↓\033[0m
  \033[1;91mControllers (HTTP)\033[0m
  \033[1;91m↓\033[0m
  \033[1;91mServices (Business Logic)\033[0m
  \033[1;91m↓\033[0m
  \033[1;91mDAO / Repositories\033[0m
  \033[1;91m↓\033[0m
  \033[1;91mDatabase\033[0m

Rules: Controllers cannot import DAOs. Services cannot import
Express. DAOs cannot import services. No circular dependencies.

\033[1;34m============================================================\033[0m

\033[1;97m2. DEFINE CORE INVARIANTS\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Create \033[1;91m/tests/invariants/\033[0m

Write tests describing truths of the system before feature impl:
- Data integrity invariants
- Authorization invariants
- Ownership rules
- Append-only guarantees
- Referential integrity assumptions

Example:

  \033[1;91mtest('user cannot access another user data', async () => {\033[0m
  \033[1;91m  const res = await request(app)\033[0m
  \033[1;91m    .get(`/api/users/${otherUserId}`)\033[0m
  \033[1;91m    .set('Authorization', userToken)\033[0m
  \033[1;91m  expect(res.status).toBe(403)\033[0m
  \033[1;91m})\033[0m

\033[1;34m============================================================\033[0m

\033[1;97m3. INSTALL TOOLING (NODE + TYPESCRIPT)\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Core testing stack:
- Vitest or Jest
- Supertest (API testing)
- Stryker (mutation testing)
- dependency-cruiser (architecture enforcement)
- ESLint (strict rules)
- Husky (pre-commit enforcement)

\033[1;34m============================================================\033[0m

\033[1;97m4. CONFIGURE LINTING AS GUARDRAIL\033[0m
\033[1;34m------------------------------------------------------------\033[0m

\033[1;91m.eslintrc\033[0m rules to prevent AI shortcuts:

  \033[1;91mno-explicit-any\033[0m
  \033[1;91mno-unused-vars\033[0m
  \033[1;91mno-console\033[0m (except allowed logger)
  \033[1;91mno-fallthrough\033[0m
  \033[1;91mno-empty\033[0m
  \033[1;91mcomplexity: max 10\033[0m
  \033[1;91mmax-depth: 3\033[0m
  \033[1;91mmax-lines-per-function: 50\033[0m

Optional strong rule:

  \033[1;91m"no-warning-comments": ["error", { "terms": ["todo", "fixme"], "location": "anywhere" }]\033[0m

No TODOs survive.

\033[1;34m============================================================\033[0m

\033[1;97m5. ENFORCE ARCHITECTURE BOUNDARIES\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Install dependency-cruiser. Create \033[1;91m.dependency-cruiser.js\033[0m

Example rule:

  \033[1;91m{\033[0m
  \033[1;91m  name: 'no-controller-to-dao',\033[0m
  \033[1;91m  from: { path: '^src/controllers' },\033[0m
  \033[1;91m  to: { path: '^src/dao' },\033[0m
  \033[1;91m  severity: 'error'\033[0m
  \033[1;91m}\033[0m

CI script:

  \033[1;91m$\033[0m \033[1;91mdepcruise src --config .dependency-cruiser.js\033[0m

Architecture violations fail build.

\033[1;34m============================================================\033[0m

\033[1;97m6. DEFINE TESTING LAYERS\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Scaffold test folders before writing features:

  \033[1;91m/tests\033[0m
  \033[1;91m  /unit\033[0m
  \033[1;91m  /integration\033[0m
  \033[1;91m  /routes\033[0m
  \033[1;91m  /security\033[0m
  \033[1;91m  /performance\033[0m
  \033[1;91m  /invariants\033[0m

Even if empty initially. Forces mental separation.

\033[1;34m============================================================\033[0m

\033[1;97m7. SECURITY BASELINE TESTS\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Create \033[1;91m/tests/security/auth.test.ts\033[0m

Include: unauthenticated → 401, unauthorized → 403, SQL injection
fails, XSS sanitized, invalid JSON rejected. These tests exist
before endpoints exist. They will fail — that is correct.

\033[1;34m============================================================\033[0m

\033[1;97m8. QUERY COUNT INSTRUMENTATION\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Wrap database client with query counter in test mode:

  \033[1;91mglobal.queryCounter = 0\033[0m
  \033[1;91mdb.on('query', () => { global.queryCounter++ })\033[0m

Performance test template:

  \033[1;91mtest('endpoint does not exceed query budget', async () => {\033[0m
  \033[1;91m  global.queryCounter = 0\033[0m
  \033[1;91m  await getDashboard()\033[0m
  \033[1;91m  expect(global.queryCounter).toBeLessThanOrEqual(5)\033[0m
  \033[1;91m})\033[0m

Prevent N+1 before it exists.

\033[1;34m============================================================\033[0m

\033[1;97m9. PERFORMANCE BUDGET TEMPLATE\033[0m
\033[1;34m------------------------------------------------------------\033[0m

  \033[1;91mtest('operation under 100ms', async () => {\033[0m
  \033[1;91m  const start = performance.now()\033[0m
  \033[1;91m  await runOperation()\033[0m
  \033[1;91m  const duration = performance.now() - start\033[0m
  \033[1;91m  expect(duration).toBeLessThan(100)\033[0m
  \033[1;91m})\033[0m

Define budget early. Do not wait until performance is "bad".

\033[1;34m============================================================\033[0m

\033[1;97m10. MEMORY LEAK TEMPLATE\033[0m
\033[1;34m------------------------------------------------------------\033[0m

  \033[1;91mtest('no memory growth over repeated calls', async () => {\033[0m
  \033[1;91m  const before = process.memoryUsage().heapUsed\033[0m
  \033[1;91m  for (let i = 0; i < 50; i++) { await runOperation() }\033[0m
  \033[1;91m  const after = process.memoryUsage().heapUsed\033[0m
  \033[1;91m  expect(after - before).toBeLessThan(5_000_000)\033[0m
  \033[1;91m})\033[0m

Signals: resource safety matters.

\033[1;34m============================================================\033[0m

\033[1;97m11. MUTATION TESTING EARLY\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Install Stryker. Run once on empty or small logic. Goal: > 80%
mutation score before expanding codebase. Mutation testing prevents
false confidence.

\033[1;34m============================================================\033[0m

\033[1;97m12. CI BEFORE FEATURE WORK\033[0m
\033[1;34m------------------------------------------------------------\033[0m

GitHub Actions minimal pipeline:
1. Install dependencies
2. Lint
3. Run architecture checks
4. Run unit tests
5. Run integration tests
6. Run mutation testing (optional nightly)

Every PR must pass. No exceptions.

\033[1;34m============================================================\033[0m

\033[1;97m13. BRANCH DISCIPLINE\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Before coding: each change atomic, each atomic change adds or
modifies tests, no multi-feature branches, one invariant per issue
if possible. Prevents change 789 from touching 49 and 140 silently.

\033[1;34m============================================================\033[0m

\033[1;97m14. REGRESSION RATCHET\033[0m
\033[1;34m------------------------------------------------------------\033[0m

After first stable milestone: snapshot API contracts, DB schema,
performance numbers. Add \033[1;91mexpect(response).toMatchSnapshot()\033[0m
Snapshots freeze contracts; they are not laziness.

\033[1;34m============================================================\033[0m

\033[1;97m15. FEATURE FLAGS BY DEFAULT (OPTIONAL)\033[0m
\033[1;34m------------------------------------------------------------\033[0m

All new features behind flags. Reduces blast radius.

\033[1;34m============================================================\033[0m

\033[1;97m16. FAIL LOUD RULE\033[0m
\033[1;34m------------------------------------------------------------\033[0m

No silent fallback. In code: no \033[1;91m|| []\033[0m unless justified,
no try/catch that swallows errors, all unexpected states throw.

  \033[1;91mtest('invalid state throws', async () => {\033[0m
  \033[1;91m  await expect(invalidCall()).rejects.toThrow()\033[0m
  \033[1;91m})\033[0m

Protects against AI smoothing over logic gaps.

\033[1;34m============================================================\033[0m

\033[1;97m17. COMMIT HOOKS\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Use Husky: block commit if lint fails, block if tests fail,
optional: block if mutation score drops. Build friction intentionally.

\033[1;34m============================================================\033[0m

\033[1;97m18. DOCUMENTATION BEFORE EXPANSION\033[0m
\033[1;34m------------------------------------------------------------\033[0m

Before adding feature group: update architecture doc, invariant list,
performance budget doc. Do not expand blind.

\033[1;34m============================================================\033[0m

\033[1;97m19. CHECKLIST BEFORE CODING "IN ANGER"\033[0m
\033[1;34m------------------------------------------------------------\033[0m

- Architecture documented
- Dependency rules enforced
- Invariant tests written
- Security baseline tests exist
- Performance budget defined
- Query counter integrated
- Mutation testing configured
- CI pipeline active
- Pre-commit hooks installed

Only then write feature code.

\033[1;34m============================================================\033[0m

\033[1;97mWHY THIS WORKS\033[0m
\033[1;34m------------------------------------------------------------\033[0m

AI agents optimise for passing tests and prompt completion. If your
tests encode architectural direction, performance budgets, security
invariants, and mutation resistance, then AI is constrained into
safe implementation. You are not fighting the agent — you are
boxing it into correctness.
