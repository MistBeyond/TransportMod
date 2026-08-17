---
name: document-editing-rules
description: Mandatory rules for modifying any file under docs/ or .agents/skills/. Use when editing, revising, adding, or deleting documentation, ADRs, roadmaps, architecture docs, or skill docs.
---

# Document Editing Rules

## Runner Requirement

**Must run scripts with `uv` first, or `python`:**

- `uv run scripts/new_docs_subdir.py <subdir>`
- `python scripts/new_docs_subdir.py <subdir>`

Prefer `uv` over `python`. Do not execute the scripts with any other interpreter.

## 0. Scope (mandatory)

- Any agent that modifies any file under `docs/` or `.agents/skills/` MUST read and follow this document before making
  the change — regardless of change size and regardless of whether the agent also has other tasks.
- Read-only access to documentation is not covered by this document.
- This skill is loaded on demand; it is not a globally injected rule set.

## Creating a New Docs Subdirectory

When opening a new documentation subdirectory under `docs/`, generate its README with the script:

1. Run `uv run scripts/new_docs_subdir.py <subdir>` (or `python scripts/new_docs_subdir.py <subdir>`), where
   `<subdir>` is a relative path under `docs/`, e.g. `roadmap/rail` or `features/foo`.
2. The script validates the path strictly and refuses to run otherwise: it rejects absolute paths, `..`/`.` path
   components, drive letters, `~`, Windows-forbidden characters, subdirs starting with the docs root name (e.g. `docs`
   or `docs/rail`), paths escaping `docs/`, and any existing target README (it never overwrites).
3. Run `--dry-run` first to preview the target path and title before actually creating anything:
   `uv run scripts/new_docs_subdir.py <subdir> --dry-run`.
4. After generation, **fill in every `_TODO_` section** (Purpose, Overview table, Content Ownership table, Ownership
   Rules) before delivering. Never leave an unfilled TODO.
5. Ownership Rules follow **progressive disclosure**: write rules for THIS directory only — do not copy the parent
   directory's rules (`docs/README.md` owns the global ones) and do not define what subdirectories may contain (each
   subdirectory's own README owns that). Rules at different levels stay implicitly consistent, not explicitly copied.
6. After creating the directory, check ownership and index sync per sections 2-5 of this document.

## 1. Responsibility Separation (single source of truth)

- **Change docs without changing code; change code without changing docs.** A single task should not mutate both the
  documentation and the code, to avoid creating multiple authoritative sources or mid-task source drift.
- Dedicated documentation agents: this applies fully. When documentation and code disagree, record the mismatch and tell
  the user so the code owner can fix it; do not change code yourself.
- Mixed agents (allowed to change code too): the task's primary goal decides which side to touch. When a change
  genuinely spans both sides (e.g. `docs/features/` specs are coupled with code), both may be edited together, but the
  two sides MUST stay consistent.
- Never silently fix only one side of a doc/code mismatch — make the user aware.
- Global prohibitions (build scripts, dependencies, downloads, handwritten generated resources, absolute paths,
  commits) are NOT repeated here; follow root `AGENTS.md` → Prohibited Actions / Tool Usage.

## 2. Document Hierarchy and Conflict Priority

- Hierarchy: binding contracts (`docs/roadmap/rail/runtime-contract.md`, ADRs) > `docs/architecture.md` >
  `docs/design-principles.md` > roadmap sub-documents > placeholder docs.
- Changing a higher-level document requires checking lower-level references; lower-level changes must not conflict with
  higher-level ones. On conflict, the higher level wins (see "this document wins" in
  `docs/roadmap/rail/runtime-contract.md`).
- State before editing: which level you are changing and which change type applies (see section 6).

## 3. Pre-Edit Workflow

1. Read the three entry documents — `docs/roadmap.md` (what to build, in which order), `docs/architecture.md` (where
   things belong, recorded rulings), `docs/design-principles.md` (how to design) — plus any relevant roadmap
   sub-documents and ADRs.
2. **Search the full impact surface (bilingual)**: documentation is a mix of English and Chinese; grep MUST cover both
   languages (e.g. `8-direction|8 方向|all 8|direction values`). List every affected file before editing.
3. **Verify current code facts (read-only, via IDEA MCP)**: numbers, enums, APIs, and resource files must match the code
   (e.g. the collision strip width 26 px derives from the model gauge 24 px plus two 2 px rails; verify API symbols
   exist with `search_symbol`).
4. **On ambiguity, contradiction, or an unrecorded ruling: ask the user first** — do not decide unilaterally (see
   `docs/architecture.md` → How to use).

## 4. Editing Rules

- Base numbers on code facts and state the derivation; never invent values.
- Keep terminology consistent across all documents (e.g. `World Grid`, simple/complex cell, `TrackPlacement`, gauge,
  baked-model route, `direction` property).
- Update every document that references the changed concept; leave no old wording behind.
- When a MUST-level clause of a binding contract changes, update the corresponding acceptance scenarios in
  `docs/roadmap/rail/runtime-contract.md`.
- ADR revisions: never rewrite an accepted decision's body — append a `Revised: <yyyy-MM-dd> — <summary>` note only (see
  `docs/decisions/README.md` for the ADR mechanism).
- New decisions: ask the user (with concrete options) → on confirmation create a new ADR (`000X-<topic>.md` with
  Status/Date/Context/Decision/Consequences/Related Documents) → update the index in `docs/decisions/README.md`.
- `docs/features/`: specs exist only when the user explicitly created or maintains them; do not create, update, or
  delete feature specs without the user's approval (see `docs/architecture.md` → Feature specs, which is authoritative).
- Never overturn a ruling the user has explicitly made without the user's consent.

## 5. Post-Edit Review (mandatory)

- grep for old wording: **zero residue** (check both English and Chinese).
- New wording is consistent across all affected documents (same numbers, phrasing, and reference paths).
- Cross-references actually exist (e.g. `docs/decisions/000X-*.md`, `docs/roadmap/rail/*.md`).
- Indexes stay in sync: `docs/decisions/README.md` matches the ADRs; the related-documents list in `docs/roadmap.md`
  matches reality.

## 6. Change Types

| Type       | Definition                                 | Requirements                                                                                                  |
|------------|--------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Revision   | Semantic change to existing content        | Add a `Revised` note (date + summary) to contracts/ADRs; update all affected docs                             |
| Correction | Align with code facts / existing agreement | State the basis; show the derivation for numbers                                                              |
| Addition   | New section / new document / new ADR       | Follow the document hierarchy; new ADRs go through the decision flow                                          |
| Deletion   | Removing content                           | Confirm nothing references it first; mark superseded content with a note instead of silently deleting history |

## 7. Authoritative Rule Index

| Rule                                            | Authoritative location                                          |
|-------------------------------------------------|-----------------------------------------------------------------|
| Global prohibitions (no code/build/deps/...)    | Root `AGENTS.md` → Prohibited Actions / Tool Usage              |
| Recording rulings; ask the user when unrecorded | `docs/architecture.md` → How to use                             |
| Feature specs approval                          | `docs/architecture.md` → Feature specs                          |
| ADR mechanism and index                         | `docs/decisions/README.md`                                      |
| Document hierarchy / conflict priority          | `docs/roadmap/rail/runtime-contract.md`, `docs/architecture.md` |
| Three entry documents                           | Root `AGENTS.md` → Docs Map                                     |
