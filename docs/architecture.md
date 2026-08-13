# Architecture Map

This document is the authoritative source for concrete package placement and the user's architecture rulings. Read it
before any structural change: new packages, moving or extracting classes, or changing dependency direction. General
design principles live in `docs/design-principles.md`; this map records where things actually belong.

## How to use

- Before a structural change, check this map first.
- If the placement is documented, follow it without redesigning.
- If it is not documented, ask the user with concrete options (for example: "put this helper in `api` (recommended) or
  `core`?"). After the user confirms, record the decision in `docs/decisions/README.md`.
- A recorded user decision overrides generic best practice. Do not invent package boundaries.

## General baseline

- `com.mistbeyond.transport` is the main mod root; the `@Mod` entry belongs here and should not contain feature logic.
- Generated assets such as models, blockstates, language files, tags, recipes, and loot tables are produced through
  datagen by default. Hand-written model files are not allowed except for custom templates that datagen cannot express.
- `api` is an interface-first pure contract layer. It may use standard Java, JSpecify, and foundational
  Minecraft/NeoForge types, but it must not depend on any project package. It contains service interfaces, read-only
  views, I/O records such as IDs, requests, and results, plus stable public enums. Internal domain records such as
  `RailNode`, `RailEdge`, and `RailSection` are implementation-layer candidates and should not become public data models
  unless they are intentional read-only views.
- `core` is the thick gameplay/domain layer. `core.<feature>` contains gameplay and business rules, services, state
  machines, feature public entry points, lifecycle logic, and `api` implementations. `core` depends on `api`, `config`,
  and `util`, and may use `internal` as implementation practice requires.
- Track graph collection and reachability are `core.rail` domain concerns. Content packages expose world state through
  `api.rail.graph.TrackGraphSource`; blocks and items do not own graph construction or reachability searches.
- `api.rail.graph.TrackCellData` is the read-only track cell contract. It contains track placements and signal placement
  information.
- Track cells use one block ID with an 8-direction `direction` property. Simple cells have no block entity; complex
  cells with crossings, curves, curve ramps, or signals use a block entity.
- Datagen generates simple track models; the block entity renderer renders complex cells from `TrackCellData`.
- Collision and physical occupancy are generated from `TrackCellData`, clipped to the owning block's 16x16 bounds, and
  unioned for complex cells. Track cells occupy one block slot and do not support waterlogging.
- Visual overflow does not create neighboring occupancy, collision, or graph connections. Train entities ignore track
  collision and use entity AABBs against ordinary blocks and other entities.
- `internal` is a minimal implementation-detail container. `internal.<feature>` exists for implementation details that
  do not yet fit `core`. Its exact boundaries and dependency rules will be defined by code practice, not documented in
  advance.
- `config` is a leaf package. It depends on `api`/`util` and can be used by `core`, content packages, and integration.
- `block`, `item`, `entity`, `client`, `inventory`, and `recipe` are final presentation/consumer packages. They can
  depend on
  `api`, `core`, `config`, and `util`; direct `internal` use is allowed only when it emerges from concrete code needs.
  `core` must not depend on them.
- `screen` lives under `client.screen`; it is not a separate top-level package.
- Do not use NeoForge `@OnlyIn` because NeoForge does not recommend it; client-only code lives under `client` packages
  and is wired through client entry points.
- `integration` contains JEI, Jade, KubeJS, and other external mod/addon integration. It can depend on `api`, `core`,
  `config`, `util`, and content packages as needed. Direct `internal` use is decided case by case as code develops.
- KubeJS defaults to `api`; it can depend on `core` without registration, and direct `internal` usage is rare and must
  be logged in code comments and `docs/decisions/README.md`.
- `docs/roadmap.md` defines code implementation order. Roadmap node detail lives in `docs/roadmap/` and is read on
  demand from the roadmap. `docs/features/` remains reserved for code-specific feature specs.

## Package map

| Package                    | Responsibility                                                                                                                    | Belongs here                                                                                                            | Does not belong here                                                                                                                                  |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `com.mistbeyond.transport` | Main mod entry and initialization                                                                                                 | `@Mod` class and mod lifecycle                                                                                          | Individual feature gameplay implementations                                                                                                           |
| `api`                      | Pure interface-first cross-feature and external contracts                                                                         | `api.<feature>` service interfaces, read-only views, ID/request/result records, stable public enums, `TrackGraphSource` | Dependencies on project packages; internal domain records such as `RailNode`, `RailEdge`, `RailSection`; internal state enums; persistence structures |
| `core`                     | Thick gameplay/domain layer: business rules, services, state machines, feature public entry, lifecycle, and `api` implementations | `core.<feature>`                                                                                                        | Dependencies on `block`, `item`, `entity`, `client`, `inventory`, `recipe`, or `integration`                                                          |
| `internal`                 | Minimal implementation-detail container; exact boundaries to be defined by code practice                                          | `internal.<feature>`                                                                                                    | Being treated as a finalized architecture layer or public contract package                                                                            |
| `config`                   | Mod configuration and user-facing settings; leaf dependency available to core, content, and integration                           | `config` contracts and settings values                                                                                  | Dependencies on `block`, `item`, `entity`, `client`, `inventory`, `recipe`, `core`, or `integration`                                                  |
| `util`                     | Generic helpers without business semantics                                                                                        | Shared utilities                                                                                                        | Dependencies on feature internals                                                                                                                     |
| `block`                    | Final Minecraft block content and presentation: block states, block entities, world interaction                                   | Blocks, block entities, and world-to-`TrackGraphSource` adaptation                                                      | Domain and business logic ownership, graph collection, reachability searches                                                                          |
| `item`                     | Final Minecraft item content and presentation: items, tools, registry-facing types                                                | Items and tools                                                                                                         | Domain and network logic ownership                                                                                                                    |
| `entity`                   | Final Minecraft entity content and presentation: entity types, movement/rendering glue                                            | Entities and entity registry types                                                                                      | Domain and business logic ownership                                                                                                                   |
| `client`                   | Client rendering, GUI, and screens; `client.screen` subpackage                                                                    | UI and visual concerns                                                                                                  | Server-authoritative logic                                                                                                                            |
| `inventory`                | Inventory and loading/unloading presentation/consumers: slots, cargo interaction, UI hooks                                        | Inventory/cargo UI and content interaction                                                                              | Core domain logic ownership                                                                                                                           |
| `recipe`                   | Recipes and datagen-related helpers: recipe definitions, tags, generated assets                                                   | Recipe definitions and generation                                                                                       | Core domain logic ownership                                                                                                                           |
| `integration`              | JEI, Jade, KubeJS, and other external mod/addon integration; may use api/core/config/util/content packages                        | External integration code                                                                                               | Top-level integration packages outside `integration`                                                                                                  |
| `integration.kubejs`       | KubeJS plugin discovery, bindings, events, and type wrappers; defaults to api, may use core, logs rare internal exceptions        | Script integration                                                                                                      | Direct `internal` usage without a logged exception                                                                                                    |

## Decisions

Current architecture decisions are stored in `docs/decisions/README.md`.

## Feature specs

`docs/features/` holds user-authored, code-specific specs. A spec is active only when the user explicitly created or
maintains it; active specs declare their own scope and are referenced from the code they cover (e.g., code Javadoc).
Read an active spec before modifying the code it covers. Do not create, update, or delete feature specs without the
user's approval.
