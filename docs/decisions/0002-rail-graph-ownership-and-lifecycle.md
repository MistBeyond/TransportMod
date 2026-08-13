# ADR 0002: Rail Graph Ownership and Lifecycle

Status: Accepted

Date: 2026-08-14

## Context

The current rail prototype uses a static `ConcurrentHashMap` in `core.rail.RailNetworkService` with no world lifecycle,
no persisted graph cache, and no validation path. The railway MVP needs a defined owner for graph state, a defined
persistence model, and a defined rebuild strategy before automatic dispatch can be implemented safely.

## Decision

- Add `core.rail.RailNetworkManager` as the per-`ServerLevel` runtime owner of rail graph, dispatch, train aggregates,
  dirty tracking, validation, and save/load coordination.
- Treat world blocks as the authoritative source of truth. The saved graph is a cache, not the source of truth.
- Persist graph cache, train aggregate, timetable, and dispatch state through `internal.rail`.
- Build graph state lazily on first use around nearby trains or players; do not scan the whole world.
- Mark normal track or signal edits as dirty. Validate when a train or player is near the relevant loaded region.
- Validate local tracks plus neighboring sections; rebuild the affected connected component when a mismatch is found.
- Validate edits in unloaded chunks when the chunk is loaded; do not keep a global dirty queue.
- Do not use a static map or cross-world mutable graph ownership.

## Consequences

- `core.rail` owns runtime rail state and lifecycle; `internal.rail` owns persistence implementation.
- Automatic routes must be revalidated or rerouted after graph changes.
- The existing static `RailNetworkService` will be replaced when the implementation contract is implemented.
- The MVP does not need incremental graph updates; validation can rebuild the affected connected component.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/decisions/0003-rail-runtime-threading-and-snapshots.md`
- `docs/decisions/0004-rail-signals-dispatch-and-train-aggregate.md`
