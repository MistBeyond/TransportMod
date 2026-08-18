# ADR 0007: Rail Path Signals and Blocked-Train Wake-up

Status: Accepted

Date: 2026-08-17

Revised: 2026-08-17 — Path-signal placement refined by ADR 0008: instead of "the junction-entrance track cell", a path
signal may be placed on any cell of an approach chain whose facing side eventually reaches a routing node.

## Context

ADR 0004 and `docs/roadmap/rail/runtime-contract.md` established path signals as directional junction-entrance signals
that reserve the full conflict path through a junction until the train exits. Several surrounding behaviors were left
undefined and carried implementation risk:

- how the player chooses between block and path signals, and where a path signal is placed;
- what a path signal's `RED`/`GREEN` display is based on (only `RED`/`GREEN` are player-visible);
- whether multiple trains may traverse a junction concurrently;
- how a blocked automatic train is reactivated after "stop and wait" (the contract only defined graph-change reroute as a
  restart trigger, which would leave trains parked after a signal turned green);
- the deadlock level beyond stop-and-wait.

These gaps were reviewed and resolved through explicit user rulings (this session).

## Decision

- Block signals and path signals are separate placeable signal items; the player chooses which type to place. A path
  signal is placed at the junction-entrance track cell, facing the junction, not on the exit track.
- A conflict path is a path that cannot be reserved concurrently with another train path under the junction conflict
  rules (for example, paths sharing a segment or edge, or crossing at the same routing node). The example list is not
  exhaustive; the exact conflict geometry is deliberately left open.
- A path signal is evaluated only for automatic trains, on the route locked at departure. It displays `GREEN` only when
  the required reservation for the train's locked route through the junction can be granted atomically; otherwise it
  displays `RED`. `GREEN` is reservation eligibility, not a promise: the reservation grant itself is atomic, and the
  losing train stops at the section boundary.
- Trains whose paths do not conflict may traverse a junction simultaneously; a given conflict path admits one train at a
  time.
- Once a junction route reservation is granted, the related entrance signals turn `RED` until the train's tail exits
  the junction. Tail computation and per-segment/partial release are left open.
- A blocked automatic train stops and waits, and MUST retry advancement after a reservation, occupancy, or signal-state
  change that may unblock it. Implementations SHOULD use event-driven wake-up and MAY keep a low-frequency fallback
  retry. This is a behavior-level contract, not a tick-architecture requirement.
- Automatic deadlock breaking is a future extension; cyclic deadlocks involving multiple junctions are outside the scope
  of automatic resolution and MUST be resolved through player actions (no dedicated tool is provided beyond the derail
  reset).
- Manual trains are not signal-constrained (existing ruling, unchanged).

## Consequences

- Signal semantics are now fully specified in `docs/roadmap/rail/sections.md`; MUST-level clauses and acceptance
  scenario 28 were added to `docs/roadmap/rail/runtime-contract.md`.
- Implementation must add `PATH`-specific behavior (the current `SignalStateResolver` treats `PATH` like `BLOCK`), an
  atomic junction reservation grant, and a dispatch change-flag consumed by blocked automatic trains. The automatic
  train movement layer does not exist yet and is the dominant cost; the decisions above only add a small increment on
  top of it.
- The exact junction conflict geometry and release granularity remain open and are intentionally not frozen by this
  decision.
- For this milestone, cyclic deadlock is resolved by the player; automatic breaking is deferred to a future extension.

## Related Documents

- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/dispatch.md`
- `docs/decisions/0004-rail-signals-dispatch-and-train-aggregate.md`