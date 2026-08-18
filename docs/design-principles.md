# Design Principles

This document explains the project-specific design principles referenced from `AGENTS.md`. They guide new code and
refactoring; read this document when a design decision is ambiguous or an exception seems justified. The `AGENTS.md`
section stays short by design; the rationale lives here.

## 1. Contracts live in `api`, `core` carries gameplay, `internal` is a placeholder for implementation details

- **Why**: Stable contracts should not be tied to implementation details, and `core` should own the gameplay/domain
  layer directly rather than being a thin wrapper.
- **How to apply**: `api.<feature>` defines service interfaces and read-only views. Records are limited to IDs,
  requests, and result snapshots. Stable public enums are allowed. Domain records such as `RailNode`, `RailEdge`, and
  `RailSection` live in `core`/`internal`. `core.<feature>` contains gameplay and business rules, services, state
  machines, feature public entry points, lifecycle logic, and `api` implementations. `internal.<feature>` is a minimal
  placeholder for implementation details that do not yet fit `core`.
- **Example**: `api.rail` exposes `RailGraphView`, `DispatchService`, and `RailPathfinder` instead of directly exposing
  `RailNode`/`RailEdge` records. `core.rail` owns the implementation and graph state.
- **When to break**: Entities, blocks, items, and block entities are framework types and belong to content packages. They
  may use `core`; `core` must not depend on them. `internal` use can be added when code practice supports it.

## 2. Dependencies stay acyclic

- **Why**: A stable, one-way dependency direction keeps the architecture understandable and avoids initialization
  problems.
- **How to apply**: `api` depends on no project packages. `core` depends on `api`, `config`, and `util`. Content
  packages such as `block`, `item`, `entity`, and `client` are final presentation/consumers and can depend on `core`;
  `core` must not depend on them. `internal`
  dependency rules are left open until code practice defines them.
- **Example**: A rail block calls `core.rail` entry points instead of making `core.rail` depend on `block`.
- **When to break**: Do not break this for new code. If a cycle is needed, extract shared logic into `api`, `config`, or
  `util` first.

## 3. Prefer composition to inheritance

- **Why**: NeoForge/Minecraft classes are heavy and tied to lifecycle hooks. Composition keeps behavior small, focused,
  and testable without framework instances.
- **How to apply**: Build behavior from fields and helper components; use inheritance only where the framework requires
  a subclass. Keep classes non-final by default: other mods may legitimately extend our classes, so `final` is
  reserved for a concrete reason such as an invariant that subclassing would break (ADR 0009). Data carriers stay
  records, which are implicitly final and unaffected by this rule.
- **Example**: Machine logic and energy networks live in plain classes; blocks and block entities subclass framework
  types only at the boundary.
- **When to break**: Extension points such as blocks, items, block entities, and Mixin targets require subclassing
  framework types.

## 4. Prefer immutable data and explicit nullability

- **Why**: Immutable data removes whole classes of shared-state bugs, and JSpecify nullability is the project-wide
  contract for APIs.
- **How to apply**: Use records or final fields with fully initialized state in new code. Every package is
  `@NullMarked` via `$ensure-package-info`; avoid null literals and shared mutable state.
- **Example**: Data carriers use records or final fields, and each package has a `package-info.java` annotated with
  `@NullMarked`.
- **When to break**: Framework callbacks and block entity sync require mutable state or lifecycle fields. Keep such
  state minimal and local.

## 5. Cross-feature access goes through public APIs

- **Why**: Stable contracts reduce coupling; implementation details should be free to change.
- **How to apply**: Prefer `api` for cross-feature and external access. Content packages call `core.<feature>` public
  entry points. Direct `internal` access is allowed when concrete code needs it, but should remain deliberate.
- **Example**: Integration code reads rail state through `api.rail`; when the contract is not enough, it may use
  `core.rail`. Direct use of `internal.rail` is evaluated case by case.
- **When to break**: Framework registration or lifecycle hooks may need direct concrete access. Document these cases
  and keep them rare.

## 6. External integrations live in `integration`

- **Why**: Addon and third-party APIs have their own lifecycles and optional loading rules; isolating them prevents
  feature code from depending on optional mods.
- **How to apply**: JEI, Jade, KubeJS, and all other external mod/addon integration content goes under `integration`.
  Do not create top-level addon packages outside it.
- **Example**: KubeJS plugin code lives under `integration.kubejs`. It defaults to `api`, may depend on `core` without
  registration, and logs rare direct `internal` exceptions.
- **When to break**: Do not break this for new code. If integration logic becomes shared, extract it into `api` or
  `core` first.

## 7. Do not use NeoForge `@OnlyIn`

- **Why**: NeoForge does not recommend `@OnlyIn`.
- **How to apply**: Keep client-only logic in `client` packages and wire it through client lifecycle/entry points.
  Do not annotate methods with `@OnlyIn`.
- **When to break**: Do not break this for new code.

## Known debt

`core` is intentionally thick and `internal` is intentionally under-defined. Let implementation practice reveal the
boundary as the codebase grows.
