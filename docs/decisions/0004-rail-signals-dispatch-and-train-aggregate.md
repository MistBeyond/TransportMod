# ADR 0004: Rail Signals, Dispatch, and Train Aggregate

Status: Accepted

Date: 2026-08-14

## Context

The rail system needs signal semantics, automatic scheduling, and a train model that supports both manual and automatic
driving without creating two separate implementations.

## Decision

- Use one `core.rail.RailTrainAggregate` for all trains. Manual and automatic are `RailControlMode` values.
- Manual mode is driven by player input; automatic mode is driven by `RailTrainSchedule`.
- Switching to manual pauses the automatic schedule and releases its route/reservations.
- Use explicit `ONE_WAY` and `LOOP` schedule types with ordered generic stops. Stops contain a track node ID plus generic
  settings for future expansion.
- Schedules are player-started, persisted, editable while running, and reject empty or single-stop lists.
- Use directional block signals that protect the next section in their facing direction.
- Use directional path signals that reserve the full conflict path through a junction until the train exits.
- Automatic routes lock at departure and reserve/release sections stepwise.
- Graph changes trigger reroute when possible; otherwise the train stops and waits.
- Manual trains may ignore signals. Collisions derail all involved trains, pause schedules, and release routes.
- A derailed train keeps its current section locked until the train is reset and leaves the section. Other automatic
  trains reroute around the locked section when possible.
- Train damage and repair are not planned gameplay; a player tool resets derailed trains.

## Consequences

- Manual and automatic trains share the same movement, graph, persistence, and collision logic.
- Dispatch and signal behavior can be tested through the aggregate model without requiring full entity simulation.
- The timetable contract can be extended later by adding stop settings without changing the train model.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
