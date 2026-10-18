# CLAUDE.md

Project conventions for **JSONViewer** — open a JSON file, pretty-print it,
and display it line by line with syntax highlighting. Claude Code reads this
file automatically at session start. Keep it short and actionable — not a
marketing description (that's `README.md`).

For the working agreement, see `WHY_CLAUDE.md`. For Material 3 rules, see
`MATERIAL3RULES.md`. For test conventions, see `app/src/test/CLAUDE.md` and
`app/src/test/java/com/github/reygnn/json_viewer/TESTING_CONVENTIONS.kt`.

Cross-project conventions (stack baseline, git workflow, test philosophy,
code language) live in `~/.claude/CLAUDE.md` and are loaded automatically.
This file only holds what is specific to this project.

---

## Scope

A read-only JSON viewer. The user picks a file through the Storage Access
Framework, or opens one from another app ("Open with" — an `ACTION_VIEW`
filter for `application/json` / `text/json`, so the viewer can be set as the
default JSON handler); the app reads it, pretty-prints it, and renders it line by line.
A per-line tokenizer drives syntax highlighting (keys, strings, numbers,
booleans, null, punctuation each get their own colour).

Deliberately **not** in scope without explicit user agreement:

- Editing or writing JSON back out — this is a viewer, not an editor.
- Network fetching — files come from local storage / SAF only.
- A collapsible tree view, search, or multi-file tabs.
- Any persistence — the viewer is stateless between launches.

A new feature category needs explicit agreement before it is built.

---

## Stack particulars

Differences from the shared baseline (Kotlin + Compose + Material 3,
SDK 36, JDK 21, JUnit 4 + MockK + Turbine, manual DI, `android.util.Log`):

- **Two JSON paths, on purpose:** `org.json` (`JSONTokener` /
  `JSONObject.toString(4)`) does the pretty-printing in the repository;
  `kotlinx-serialization-json` is available for typed parsing. The
  display path uses `org.json`, not kotlinx-serialization.
- **Custom `JsonTokenizer`** (`core/parser`) does per-line lexing for
  syntax highlighting — it is not a validating parser and runs over one
  already-formatted line at a time.
- **No persistence layer** — no SharedPreferences, no DataStore, no Room.
  Nothing to store; the viewer holds state only in the ViewModel.
- **SAF for file access** — `ActivityResultContracts.OpenDocument` filtered
  to `application/json` + `application/octet-stream`, no runtime storage
  permissions. Reads are size-capped to guard against sources that report
  no (or a wrong) size.

---

## Architecture

Single Gradle module (`:app`), light clean-architecture layering. Package
root `com.github.reygnn.json_viewer`:

```
core/
  parser/      JsonTokenizer — per-line lexer for highlighting
  dispatchers/ CoroutineDispatchers interface + provider (test seam)
  util/        Resource<T> — Success / Error / Loading wrapper
domain/
  model/       JsonData, JsonLine, JsonToken / TokenType
  repository/  JsonRepository (interface)
  usecase/     LoadJsonUseCase (thin pass-through over the repository)
data/
  repository/  JsonRepositoryImpl — file read + size guard + pretty-print
ui/
  viewer/      JsonViewerViewModel (+ UiState), JsonViewerScreen, JsonHighlighter
  theme/       ColorScheme + JsonTokenColors highlight constants
```

Data flow: SAF `Uri` (or an `ACTION_VIEW` `content://` URI, handled in
`MainActivity` — `singleTop`, so `onNewIntent` reuses the open viewer) → `LoadJsonUseCase(uri)` → `JsonRepository.loadAndFormatJson`
(on `dispatchers.io`: read file → 10 MB size guard → `org.json` pretty-print → split into `List<JsonLine>`) → `Resource<JsonData>`
→ ViewModel folds it
into `JsonViewerUiState` (`StateFlow`) → the screen renders each line and
calls `JsonTokenizer` for highlight spans.

DI is manual through the `Application` class (`JsonViewerApplication` builds
the graph; `JsonViewModelFactory` injects the ViewModel). The
`CoroutineDispatchers` interface is the dispatcher injection seam for tests.

---

## Hard rules

1. **`JsonRepositoryImpl` is the single source of truth for loading.** The
   10 MB size guard, file-metadata lookup, and
   pretty-printing live there — not in the ViewModel or the use case.
2. **The tokenizer works per line, never on the whole document.** Large
   files are split first; highlighting one line at a time keeps it
   responsive. Don't refactor it into a whole-document parser.
3. **UI is a pure function of `JsonViewerUiState`.** All state transitions
   go through the ViewModel's `StateFlow`; the screen never loads or parses
   on its own.
4. **No persistence.** If a feature seems to need stored state, raise it —
   adding storage is a scope change, not an implementation detail.
5. **File I/O stays on `dispatchers.io`.** The injected `CoroutineDispatchers`
   is the only dispatcher source; never hardcode `Dispatchers.IO` in
   production code (it breaks the test seam).

---

## What this file is NOT

- Not a description of the project (see `README.md`).
- Not a TODO list.
- Not the place for transient migration notes beyond the dated banner above —
  phase detail belongs in commit messages on the migration branch.
- Not the full testing reference (see `app/src/test/CLAUDE.md` and
  `TESTING_CONVENTIONS.kt`).

Update this file when an architectural rule changes or a hard-won lesson
deserves to be future-proofed. Do not bloat it with details that are
obvious from reading the code.
