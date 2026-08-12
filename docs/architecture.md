# Architecture Map

This document is the authoritative source for concrete package placement and the user's architecture rulings. Read it
before any structural change: new packages, moving or extracting classes, or changing dependency direction. General
design principles live in `design-principles.md`; this map records where things actually belong.

## How to use

- Before a structural change, check this map first.
- If the placement is documented, follow it without redesigning.
- If it is not documented, ask the user with concrete options (for example: "put this helper in `util` (recommended)
  or `core`?"). After the user confirms, add a row to "Decision log".
- A recorded user decision overrides generic best practice. Do not invent package boundaries.

## General baseline

- `com.mistbeyond.transport` is the main mod root; the `@Mod` entry belongs here and should not contain feature logic.
- `core`/`util` are shared layers. Feature packages may depend on them, but `core`/`util` must not depend on feature
  internals.
- All external mod and addon integration belongs under `integration`, including KubeJS. `integration` may depend on
  public feature APIs, but feature and core packages must not depend on it for domain logic.
- `docs/roadmap.md` defines code implementation order. Roadmap node detail lives in `docs/roadmap/` and is read on
  demand from the roadmap. `docs/features/` remains reserved for code-specific feature specs.

## Package map

| Package                    | Responsibility                                                                             | Belongs here                                    | Does not belong here                                                           |
|----------------------------|--------------------------------------------------------------------------------------------|-------------------------------------------------|--------------------------------------------------------------------------------|
| `com.mistbeyond.transport` | Main mod entry and initialization                                                          | `@Mod` class and mod lifecycle                  | Individual feature gameplay implementations                                    |
| `core`                     | Shared domain contracts and services for freight, vehicles, routes, and transport concepts | Cross-feature contracts and testable logic      | Dependencies on `block`, `client`, `integration`, or feature internals         |
| `util`                     | Generic helpers without business semantics                                                 | Shared utilities                                | Dependencies on feature internals                                              |
| `block`                    | Concrete block implementations such as rails, stations, depots, ports                      | Rendering-independent blocks and block entities | Core domain logic ownership                                                    |
| `item`                     | Concrete item implementations such as vehicles and tools                                   | Registry-facing items                           | Domain and network logic ownership                                             |
| `client`                   | Client rendering, GUI, and client-only state                                               | UI and visual concerns                          | Server-authoritative logic                                                     |
| `integration`              | JEI, Jade, KubeJS, and other external mod or addon integration                             | Public API consumption                          | Access to feature internals or `impl` packages; top-level integration packages |
| `integration.kubejs`       | KubeJS plugin discovery, bindings, events, and type wrappers                               | Script integration for TransportMod             | Feature internals and `core` reverse dependencies                              |
| `config`                   | Mod configuration and user-facing settings                                                 | Shared config contracts                         | Transport gameplay implementation                                              |
| `inventory`                | Inventory and loading/unloading systems                                                    | Shared inventory and cargo behavior             | Feature internals                                                              |
| `recipe`                   | Recipes and datagen-related helpers                                                        | Recipe definitions and generation               | Core domain ownership                                                          |

## Decision log

| Date       | Decision                                                                                                                                                                                                                                         | Reason                                                                                                     |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| 2026-08-12 | Use `docs/roadmap.md` as the implementation-order source; keep feature-specific detail out of architecture until discussed. All external mod/addon integration, including KubeJS, belongs under `integration` and starts in the foundation node. | Keeps architecture stable while preventing top-level addon packages and feature/core reverse dependencies. |

## Feature specs

`docs/features/` holds user-authored, code-specific specs. A spec is active only when the user explicitly created or
maintains it; active specs declare their own scope and are referenced from the code they cover (e.g., code Javadoc).
Read an active spec before modifying the code it covers. Do not create, update, or delete feature specs without the
user's approval.
