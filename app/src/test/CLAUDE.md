# CLAUDE.md (test source set)

Orientation for sessions touching the unit tests. Not a technical manual
— that's `java/com/github/reygnn/json_viewer/TESTING_CONVENTIONS.kt`,
which holds the concrete patterns. This file answers the _meta_ questions:
what exists, what is deliberately left out, where to start.

The baseline stack (JUnit 4, MockK, Turbine, kotlinx-coroutines-test) and
philosophy (JVM-only by default, pure logic outside Android-runtime classes,
MockK only — no Mockito) come from `~/.claude/CLAUDE.md`. This file only
records the project's choices on top of that baseline.

---

## What exists

Mirrors the production package layout:

```
MainDispatcherRule.kt           — sets Dispatchers.Main = UnconfinedTestDispatcher
core/parser/
  JsonTokenizerTest.kt          — pure JVM, lexer types + spans
data/repository/
  JsonRepositoryImplTest.kt     — Robolectric (android.net.Uri,
                                  ContentResolver, OpenableColumns);
                                  pins the 10 MB guard + pretty-print +
                                  line split
ui/viewer/
  JsonViewerViewModelTest.kt    — Turbine for loading → success/error
                                  transitions; MainDispatcherRule
  JsonHighlighterTest.kt        — pure JVM, tokens → AnnotatedString spans
```

24 tests, all run via `./gradlew testDebugUnitTest`.

---

## What this set deliberately does NOT have

Generic defaults (no Hilt test infra, no androidTest unless instrumented
behaviour matters) are inherited from `~/.claude/CLAUDE.md`. Project-
specific call-outs:

- **No `androidTest`/instrumented set.** The legacy Espresso +
  `HiltTestRunner` set was removed — the viewer has no instrumented
  behaviour worth a device. Don't recreate it.
- **No DataStore/persistence test infra.** The app has no persistence
  layer; the DataStore section in `TESTING_CONVENTIONS.kt` is inert.
- **No contract tests.** One repository implementation; a fake/impl split
  would be theatre at this scale. Inline MockK doubles only — extract a
  fake only when a pattern repeats enough to earn it.
- **No tokenizer golden-file tests.** The tokenizer is small and
  deterministic; assert token lists inline rather than against fixtures.

---

## When you come into this project

### Reading order

1. **First** `java/com/github/reygnn/json_viewer/TESTING_CONVENTIONS.kt`
   — the technical reference. Mandatory before touching any test.
2. **Then** an existing test as a template. Pick by category:
   - For a ViewModel: `ui/viewer/JsonViewerViewModelTest.kt` (Turbine +
     `MainDispatcherRule`, mock `LoadJsonUseCase`).
   - For pure logic: `core/parser/JsonTokenizerTest.kt` (no rule, no
     mocks, just assert token spans).
   - For Robolectric-required code: `data/repository/JsonRepositoryImplTest.kt`
     (`@Config(sdk = [36])`, real `ContentResolver` via Robolectric).

### Common tasks and where they go

- New `TokenType` / lexer rule → `JsonTokenizerTest.kt`.
- New UiState transition → `JsonViewerViewModelTest.kt`.
- File-loading / size-guard / parse-error behaviour → `JsonRepositoryImplTest.kt`.

---

## Anti-patterns we've already paid for

The full catalogue (dispatcher mix-up, `MutableSharedFlow` buffer trap,
DataStore extension-function trap, `mock` prefix) is in
`TESTING_CONVENTIONS.kt`. The ones most likely to bite here:

- **Mixing dispatchers** — always `runTest(mainRule.dispatcher)`, never a
  second `StandardTestDispatcher`. The ViewModel's `launchSafe` runs on the
  injected `dispatchers.main`; mismatched schedulers make it flaky.
- **Leftover Mockito** — if you open a test still written in Mockito,
  port it to MockK (cheat sheet in `TESTING_CONVENTIONS.kt`); do not add
  more Mockito.
- **Hardcoding `Dispatchers.IO`** — production reads dispatchers from the
  injected `CoroutineDispatchers`; tests substitute it. Don't bypass the
  seam.
