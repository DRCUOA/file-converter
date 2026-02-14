# Architecture — System Boundaries

**Guardrail 1:** Define system boundaries before feature work.

---

## Layer Diagram

```
UI (JavaFX)
    ↓
MainController
    ↓
Core (BatchRunner, PipelineWorker, CloudConvertFacade, Validation, OutputNaming, RetryPolicy)
    ↓
Persistence (SettingsStore)
    ↓
Local filesystem / CloudConvert API
```

---

## Dependency Rules

| Layer | May import | May NOT import |
|-------|------------|----------------|
| **UI** (`ui/`) | `core`, `persistence`, JavaFX | CloudConvert SDK directly |
| **MainController** | `core`, `persistence`, FX models | CloudConvert SDK, `java.nio.file` for business logic |
| **Core** (`core/`) | `persistence` (AppSettings only), CloudConvert SDK | UI, JavaFX |
| **Persistence** (`persistence/`) | Jackson, `java.nio.file` | UI, Core business logic, CloudConvert |

---

## Module Ownership

| Module | Contents | Data ownership |
|--------|----------|----------------|
| `ui/` | MainApp, MainController, BatchItemFx, ConversionProfileFx | UI state only; delegates to core |
| `core/` | BatchRunner, PipelineWorker, CloudConvertFacade, Profiles, Validation, OutputNaming, RetryPolicy | Conversion logic, API calls, validation |
| `persistence/` | AppSettings, SettingsStore | Config JSON, last output dir, API key storage |

---

## External Interfaces

1. **CloudConvert API** — accessed only via `CloudConvertFacade` in `core/`
2. **Filesystem** — read via `Path`; write via temp file + atomic move (OutputNaming)
3. **User config** — read/write via `SettingsStore` only

---

## Invariants (see [invariants.md](invariants.md))

- Controllers cannot import CloudConvert SDK directly
- Core cannot import JavaFX
- Persistence cannot import core business logic
- No circular dependencies between modules
