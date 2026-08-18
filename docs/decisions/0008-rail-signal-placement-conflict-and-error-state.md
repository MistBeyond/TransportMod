# ADR 0008: Rail Signal Placement, Conflict, Release, and Error-State Rules

Status: Accepted

Date: 2026-08-17

## Context

ADR 0007 defined path-signal evaluation (atomic-eligibility `RED`/`GREEN`), concurrency, and blocked-train wake-up, but
deliberately left a set of surrounding behaviors open: exact placement geometry (which cells may host a path signal),
minimum-spacing rules, the precise conflict definition behind "non-conflicting paths", release granularity, reservation
invalidation, signal visuals, and what happens when a signal is placed incorrectly. These behaviors are needed before
implementation can start.

## Decision

- Placement of path signals: a path signal may be placed on any cell of an approach chain whose facing side eventually
  reaches a routing node (it is not placed on the exit side). The guarded junction is the nearest routing node in the
  facing direction, which is unique because a chain has no other branch. The wildcard is that a path signal on a plain
  straight whose facing side never reaches a routing node is allowed but stays in the `ERROR` state.
- At most one signal may be placed per cell per facing direction; opposite directions may share a cell. Adjacent cells
  are not distance-constrained, so zero-length sections cannot arise.
- Conflict definition (B2): two paths through a junction conflict when they share any track segment or cell, or cross
  at the same routing-node cell at the same height; different-height crossings never conflict. Only fully disjoint
  paths (different entry, different exit, no shared cell) may be traversed simultaneously; merging or splitting paths
  that share an approach or exit segment conflict. This supersedes the earlier loose "split/merge may be simultaneous"
  phrasing.
- Release granularity (B3): the junction conflict-path reservation is released as a whole once the train's tail exits
  the junction; per-segment progressive release is a future optimization.
- Invalidation (B4): reservations follow the existing graph-change and derailment contract; a train stalled inside a
  junction keeps its reservation until its tail exits or the player resolves the situation. No separate mechanism is
  required.
- Display (C1/C2): block and path signals use distinct models and colors; the F3 overlay renders path signals with a
  dedicated marker (shape/label) and colors markers by aspect or `ERROR` state. Highlighting the reserved conflict
  path is not part of this milestone.
- Error state: placement is permissive and never rejected. A signal shows the `ERROR` indicator and behaves as `RED`
  when its facing side has no track, or (path signals) its facing side never reaches a routing node. Repositioning or
  refacing restores normal indication. `ERROR` is a configuration/health indicator, not a third aspect: signal aspects
  remain `RED`/`GREEN`.

## Consequences

- The "player-visible signal state is limited to `RED` and `GREEN`" wording is amended across the rail documents to
  "aspects are limited to `RED` and `GREEN`; a misconfigured signal additionally shows an `ERROR` indicator and behaves
  as `RED`". Runtime-contract acceptance scenarios 29 (error state) and 30 (conflict definition) are added.
- Implementation must add: a `PATH` branch to signal-state resolution (including `ERROR` derivation from the local
  facing graph), an atomic junction reservation grant with whole-at-once release, two signal items with distinct
  models, a same-cell/same-direction placement constraint, and F3 marker specialization plus error markers.
- The `ERROR` indicator intentionally keeps `SignalAspect` to `{RED, GREEN}`; the error state is carried by a
  health/status field rather than a new aspect.
- Deferred as future work: precise "tail" computation and progressive release (B3), reserved-path highlighting (C2),
  and the player-facing distinction between "red but expected to clear" and "deadlock" (D4).

## Related Documents

- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/dispatch.md`
- `docs/decisions/0007-rail-path-signals-and-blocked-train-wakeup.md`