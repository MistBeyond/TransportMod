---
name: document-editing-rules
description: Mandatory rules for modifying any file under docs/ or .agents/skills/. Use when editing, revising, adding, or deleting documentation, ADRs, roadmaps, architecture docs, or skill docs.
---

# Document Editing Rules

## Runner Requirement

**Must run scripts with `uv` first, or `python`** (never any other interpreter):

- `uv run scripts/new_docs_subdir.py <subdir>`
- `python scripts/new_docs_subdir.py <subdir>`

Prefer `uv` over `python`.

## 0. Scope (mandatory)

- Any agent that modifies any file under `docs/` or `.agents/skills/` MUST read and follow this document before making
  the change — regardless of change size and regardless of whether the agent also has other tasks.
- Read-only access to documentation is not covered by this document.
- This skill is loaded on demand; it is not a globally injected rule set.

## 1. Responsibility Separation (single source of truth)

- **Change docs without changing code, and code without changing docs**: one task must not mutate both sides, to avoid
  multiple authoritative sources and mid-task source drift.
- Dedicated documentation agents: applies fully. When docs and code disagree, record the mismatch and tell the user so
  the code owner can fix it; do not change code yourself, and never silently fix only one side — make the user aware.
- Mixed agents (allowed to change code too): the task's primary goal decides which side to touch. When a change
  genuinely spans both sides (e.g. `docs/features/` specs coupled with code), both may be edited together, but the two
  sides MUST stay consistent.
- Global prohibitions (build scripts, dependencies, downloads, handwritten generated resources, absolute paths,
  commits) are NOT repeated here; follow root `AGENTS.md` → Prohibited Actions / Tool Usage.

## 2. README-First Rule

Every docs directory is governed by its README: the README is the index and the content-ownership authority of that
directory (root: `docs/README.md`, one README per subdirectory). README and documents MUST stay in sync, and the
README has priority:

- **Read the README first**: before adding, moving, renaming, or deleting any document, read the affected directory's
  README (and its parent READMEs) — it states what each entry contains and who owns what.
- **Register new documents**: adding a document requires updating its directory README (Overview and Content Ownership
  tables) in the same change; an unlisted document is not integrated into the tree.
- **Existing documents follow the README**: on conflict (missing, extra, or misplaced entries; purpose or ownership
  drift), the README wins — align the document to the README, not the other way around.
- **Conflicts go to the user**: when README and document conflict and the direction is unclear, or aligning would
  overturn established content, do not decide unilaterally — let the user arbitrate before editing either side.

Scope: README-first governs the **index/ownership layer** — which documents exist in a directory and what each entry
owns. Content-level conflicts *between documents* are resolved by the hierarchy in section 3.

## 3. Document Hierarchy and Conflict Priority

- Hierarchy: binding contracts (`docs/roadmap/rail/runtime-contract.md`, ADRs) > `docs/architecture.md` >
  `docs/design-principles.md` > roadmap sub-documents > placeholder docs.
- Changing a higher-level document requires checking lower-level references; lower-level changes must not conflict with
  higher-level ones. On conflict, the higher level wins (see "this document wins" in
  `docs/roadmap/rail/runtime-contract.md`).

## 4. Pre-Edit Workflow

1. Read the three entry documents — `docs/roadmap.md` (what to build, in which order), `docs/architecture.md` (where
   things belong, recorded rulings), `docs/design-principles.md` (how to design) — plus the affected directories'
   READMEs (per section 2), any relevant roadmap sub-documents, and ADRs. State which level you are changing and
   which change type applies (see section 7).
2. **Search the full impact surface (bilingual)**: documentation mixes English and Chinese; grep MUST cover both
   languages (e.g. `8-direction|8 方向|all 8|direction values`). List every affected file before editing.
3. **Verify current code facts (read-only, via IDEA MCP)**: numbers, enums, APIs, and resource files must match the
   code (e.g. the collision strip width 26 px derives from the model gauge 24 px plus two 2 px rails; verify API
   symbols exist with `search_symbol`).
4. **Ask the user first — do not decide unilaterally** — on ambiguity, contradiction, an unrecorded ruling, or
   overturning an explicit ruling (see `docs/architecture.md` → How to use; user consent is required).

## 5. Editing Rules

- Base numbers on code facts (per section 4 step 3) and state the derivation; never invent values.
- Keep terminology consistent across all documents (e.g. `World Grid`, simple/complex cell, `TrackPlacement`, gauge,
  baked-model route, `direction` property), and update every document that references the changed concept.
- When a MUST-level clause of a binding contract changes, update the corresponding acceptance scenarios in
  `docs/roadmap/rail/runtime-contract.md`.
- ADR revisions: never rewrite an accepted decision's body — append a `Revised: <yyyy-MM-dd> — <summary>` note only
  (see `docs/decisions/README.md` for the ADR mechanism).
- New decisions: ask the user (with concrete options) → on confirmation create a new ADR (`000X-<topic>.md` with
  Status/Date/Context/Decision/Consequences/Related Documents) → update the index in `docs/decisions/README.md`.
- `docs/features/`: specs exist only when the user explicitly created or maintains them; do not create, update, or
  delete feature specs without the user's approval (see `docs/architecture.md` → Feature specs, which is
  authoritative).

## 6. Post-Edit Review (mandatory)

- grep for old wording: **zero residue** (check both English and Chinese).
- New wording is consistent across all affected documents (same numbers, phrasing, and reference paths).
- Cross-references actually exist (e.g. `docs/decisions/000X-*.md`, `docs/roadmap/rail/*.md`).
- Indexes and READMEs stay in sync: `docs/decisions/README.md` matches the ADRs; the related-documents list in
  `docs/roadmap.md` matches reality; every directory README lists only documents that exist, and every new document
  is registered in its README (per section 2).

## 7. Change Types

| Type       | Definition                                 | Requirements                                                                                                  |
|------------|--------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Revision   | Semantic change to existing content        | Add a `Revised` note (date + summary) to contracts/ADRs; update all affected docs                             |
| Correction | Align with code facts / existing agreement | State the basis; show the derivation for numbers                                                              |
| Addition   | New section / new document / new ADR       | Follow the document hierarchy; new ADRs go through the decision flow                                          |
| Deletion   | Removing content                           | Confirm nothing references it first; mark superseded content with a note instead of silently deleting history |

## 8. Authoritative Rule Index

| Rule                                            | Authoritative location                                          |
|-------------------------------------------------|-----------------------------------------------------------------|
| Global prohibitions (no code/build/deps/...)    | Root `AGENTS.md` → Prohibited Actions / Tool Usage              |
| Recording rulings; ask the user when unrecorded | `docs/architecture.md` → How to use                             |
| Feature specs approval                          | `docs/architecture.md` → Feature specs                          |
| README-first rule (read it, keep it in sync)    | Per-directory READMEs; root `docs/README.md`                    |
| ADR mechanism and index                         | `docs/decisions/README.md`                                      |
| Document hierarchy / conflict priority          | `docs/roadmap/rail/runtime-contract.md`, `docs/architecture.md` |
| Three entry documents                           | Root `AGENTS.md` → Docs Map                                     |

## Creating a New Docs Subdirectory (how-to)

When opening a new documentation subdirectory under `docs/`, generate its README with the script (runner: see the
Runner Requirement above):

1. Preview with `--dry-run` first, then run for real: `new_docs_subdir.py <subdir>`, where `<subdir>` is a relative
   path under `docs/`, e.g. `roadmap/rail` or `features/foo`.
2. The script validates the path strictly and refuses to run otherwise: absolute paths, `..`/`.` components, drive
   letters, `~`, Windows-forbidden characters, subdirs starting with the docs root name, paths escaping `docs/`, and
   any existing target README (never overwrites) are all rejected.
3. After generation, **fill in every `_TODO_` section** (Purpose, Overview table, Content Ownership table, Ownership
   Rules) before delivering; never leave an unfilled TODO.
4. Ownership Rules follow **progressive disclosure**: write rules for THIS directory only — do not copy the parent
   directory's rules (`docs/README.md` owns the global ones) and do not define what subdirectories may contain (each
   subdirectory's own README owns that). Levels stay implicitly consistent, not explicitly copied.
5. After creating the directory, check ownership and index sync per sections 3-6.
