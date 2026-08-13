# ADR 0001: API, Core, and Internal package boundaries

Status: Accepted

Date: 2026-08-13

## Context

The project needed stable public contracts, a thick gameplay/domain layer, and a place for implementation details
without forcing every implementation into an API-facing facade. The previous architecture used `core` for shared domain
contracts and `impl` subpackages for implementations.

## Decision

- Add `api` as a pure, interface-first contract layer. It may use standard Java, JSpecify, and foundational
  Minecraft/NeoForge types, but it must not depend on any project package. Records are limited to IDs, requests, and
  result snapshots. Stable public enums are allowed. Internal domain records such as `RailNode`, `RailEdge`, and
  `RailSection` live in `core`/`internal`.
- Use `core` as the thick gameplay/domain layer. `core.<feature>` contains gameplay and business rules, services, state
  machines, feature public entry points, lifecycle logic, and `api` implementations. `core` depends on `api`, `config`,
  and `util`, and may use `internal` as code practice requires.
- Use `internal` as a minimal implementation-detail container. `internal.<feature>` exists for implementation details
  that do not yet fit `core`; exact boundaries and dependency rules are defined by code practice, not in advance.
- Treat `config` as a leaf package. It depends on `api`/`util` and can be used by `core`, content packages, and
  integration.
- Treat `block`, `item`, `entity`, `client`, `inventory`, and `recipe` as final presentation/consumer packages. They can
  depend on `api`, `core`, `config`, and `util`; direct `internal` use is decided as concrete needs appear. `core` must
  not depend on them.
- Keep `screen` under `client.screen`; it is not a separate top-level package.
- Allow `integration` to depend on `api`, `core`, `config`, `util`, and content packages as needed. Direct `internal`
  use is decided case by case.
- KubeJS defaults to `api`, may depend on `core` without registration, and logs rare direct `internal` usage in code
  comments and `docs/decisions/README.md`.

## Consequences

- `architecture.md` stays compact because full decisions live in `docs/decisions/`.
- Dependencies remain acyclic and `core` remains the main gameplay/domain layer.
- `api` stays interface-first; records do not become a public domain data model.
- `internal` stays intentionally under-defined so its boundary can emerge from implementation practice.

## Related Documents

- `docs/architecture.md`
- `docs/design-principles.md`
- `docs/roadmap/kubejs.md`
