# Docs Overview and Content Ownership

This document is the **index of the `docs/` directory**: the overview of what each direct entry contains and the
authority for what content belongs in each of them. Read it before adding or moving documentation. In short, this
README is the **metadata of the documentation tree** — documentation about the documentation: how the docs are
organized and what belongs where.

## Purpose

`docs/` is the single home for the project's documentation: it decides what to build and in which order
(`roadmap.md`), where code belongs (`architecture.md`), how new code should be designed (`design-principles.md`),
and which decisions were made (`decisions/`), plus per-mode detail documents (`roadmap/`).

## Overview

| Path                   | Purpose                                                           |
|------------------------|-------------------------------------------------------------------|
| `README.md`            | This document: directory overview and content ownership rules     |
| `roadmap.md`           | Code implementation order: what to build, and in which order      |
| `architecture.md`      | Architecture map: package placement, dependency direction, layers |
| `design-principles.md` | Design principles and their rationale                             |
| `decisions/`           | Accepted decision records (ADRs) and their index                  |
| `features/`            | User-authored, code-specific feature specs (reserved)             |
| `roadmap/`             | Transport-mode detail documents                                   |

## Content Ownership

| Document               | Belongs here                                                                    | Does not belong here                                                                               |
|------------------------|---------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `roadmap.md`           | Implementation order; node status, summary, scope, and document links           | Package placement, architecture rulings, detailed numbers                                          |
| `architecture.md`      | Package placement, dependency direction, layer boundaries, ownership of objects | User rulings (→ `decisions/`); numbers and rendering mechanics (→ contracts/ADRs); gameplay detail |
| `design-principles.md` | Design principles, rationale, exception criteria                                | Concrete package placement, feature design                                                         |
| `decisions/`           | Confirmed decisions as ADRs plus their index                                    | Unconfirmed ideas, expanded implementation detail                                                  |
| `features/`            | Code-specific specs the user explicitly created or maintains                    | Anything not created by the user                                                                   |
| `roadmap/`             | Gameplay and implementation detail for each transport mode, contracts           | Architecture rulings                                                                               |

## Ownership Rules

1. Numbers and mechanics belong in binding contracts or ADRs, not in `architecture.md`.
2. New user rulings belong in `decisions/` as a new ADR with the index updated.
3. Map content (package placement, dependency direction, layer boundaries) belongs in `architecture.md`.
4. When placement is not documented, ask the user (see `architecture.md` → How to use).
5. Changes to this directory follow root `AGENTS.md` → Docs Map and the existing workflow.
