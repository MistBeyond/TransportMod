# Design Principles

This document explains the project-specific design principles referenced from `AGENTS.md`. They guide new code and
refactoring; read this document when a design decision is ambiguous or an exception seems justified. The AGENTS.md
section stays short by design; the rationale lives here.

## 1. Public APIs are interfaces

- **Why**: Callers depend on contracts instead of concrete classes, which keeps implementations swappable and makes
  behavior easier to test and mock.
- **How to apply**: New cross-package public APIs are interfaces. Implementation classes live in an `impl` subpackage
  and stay package-private where possible.
- **Example**:  Registration contracts come from the external `registry-lib` library (`com.mistbeyond.registry` with its
  `impl` subpackage).
- **When to break**: Framework integration requires concrete types, for example blocks, items, and block entities must
  subclass NeoForge/Minecraft classes. Single-consumer internal helpers may remain plain classes.

## 2. Dependencies stay acyclic

- **Why**: A stable, one-way dependency direction keeps `core`/`util` reusable, reduces initialization order problems,
  and makes module boundaries understandable.
- **How to apply**: Feature packages (`block`, `client`, `config`, `integration`, `inventory`, `item`, `recipe`) may
  depend on `core`/`util`; `core` must not gain new reverse dependencies on feature internals.
- **Example**: Feature code imports services from `core`, while `core` does not import feature internals in new code.
- **When to break**: Do not break this for new code. When touching a known debt file, refactor it instead of widening
  the reference.

## 3. Prefer composition to inheritance

- **Why**: NeoForge/Minecraft classes are heavy and tied to lifecycle hooks. Composition keeps behavior small, focused,
  and testable without framework instances.
- **How to apply**: Build behavior from fields and helper components; use inheritance only where the framework requires
  a subclass.
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

- **Why**: Internal classes change freely; reaching into another feature's internals creates hidden coupling that breaks
  without warning.
- **How to apply**: One feature calls another only through its public interfaces or entry points. Never import another
  feature's `impl` package or internal helpers.
- **Example**: `integration` (Jade, JEI, KubeJS) consumes feature public APIs instead of feature internals.
- **When to break**: In normal cases this is not broken. If a feature needs another feature's internal behavior, extract
  the shared logic into `core`/`util` or upgrade it into a public API first.

## 6. External integrations live in `integration`

- **Why**: Addon and third-party APIs have their own lifecycles and optional loading rules; isolating them prevents
  feature code from depending on optional mods.
- **How to apply**: JEI, Jade, KubeJS, and all other external mod or addon integration content goes under `integration`.
  Do not create top-level addon packages outside it.
- **Example**: KubeJS plugin code lives under `integration.kubejs`, consumes public feature APIs, and never depends on
  feature internals.
- **When to break**: Do not break this for new code. If integration logic becomes shared, extract it into `core`/`util`
  or upgrade it into a public feature API first.

## Known debt

Here's nothing.
