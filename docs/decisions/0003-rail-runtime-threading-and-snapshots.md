# ADR 0003: Rail Runtime Threading and Snapshots

Status: Accepted

Date: 2026-08-14

## Context

The current prototype exposes mutable rail state through static service calls and uses `ConcurrentHashMap` for storage.
That makes thread ownership unclear and invites cross-thread mutation. The railway runtime needs a simple thread model
that KubeJS, rendering, and debugging can consume safely.

## Decision

- All rail runtime writes MUST execute on the server main thread.
- Expose immutable `RailNetworkSnapshot` objects for read-only access.
- Rendering, debug views, KubeJS, and external integrations MUST consume snapshots instead of mutating runtime state.
- Do not use asynchronous graph mutation in the MVP. Asynchronous work may produce immutable snapshots only when a later
  ADR explicitly allows it.
- Do not store rail runtime state in a static map.

## Consequences

- Rail state has a clear single-writer model.
- External integrations get a stable read boundary without exposing internal graph mutation.
- Full rebuild or connected-component rebuild remains a main-thread operation in the MVP.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
