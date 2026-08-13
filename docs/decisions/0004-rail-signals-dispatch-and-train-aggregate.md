# ADR 0004: Rail Signals, Dispatch, Train Aggregate, and Timetable

Status: Accepted

Date: 2026-08-14

## Context

The rail system needs signal semantics, automatic scheduling, station/platform behavior, and a train model that supports
both manual and automatic driving without creating two separate implementations.

## Decision

- Keep `SignalType.BLOCK` and `SignalType.PATH`.
- Player-visible signal state is limited to `RED` and `GREEN`; complex aspects, additional signal types, and custom
  signal states are addon extension points.
- Block signals are directional and protect the next section in their facing direction.
- Path signals are directional and reserve the full conflict path through a junction until the train exits.
- Use one `core.rail.RailTrainAggregate` for all trains. Manual and automatic are `RailControlMode` values.
- Manual mode is driven by player input; automatic mode is driven by `RailTrainSchedule`.
- Switching to manual pauses the automatic schedule and releases its route/reservations.
- Use explicit `ONE_WAY` and `LOOP` schedule types with ordered stops.
- Timetable stops reference `RailStationId` plus generic stop settings; they do not reference raw track graph nodes.
- Stations restrict stopping direction: a train may stop only while facing the station's stop direction. Other trains may
  pass through in reverse but may not reverse-stop.
- Stations themselves do not load/unload cargo and do not provide fuel or other complex station features.
- A station may have a behind-chain of freight platforms or other stations.
- When a train stops at a station, all freight platforms in that station's behind-chain participate in the stop.
- Each freight platform has a binary operation in the main mod: `LOAD` or `UNLOAD`. The operation installs or removes a
  whole cargo body, excluding the underframe.
- The main departure condition is `OPERATION_COMPLETE`: the train departs after all participating freight platforms
  complete their operations.
- If a station has no freight platforms behind it, the train stops and then departs.
- If a freight platform cannot complete its operation, the train waits until the operation can complete.
- `StationOperation` and `DepartureCondition` are extension points for addons; the main mod registers only
  `LOAD`/`UNLOAD` and `OPERATION_COMPLETE`.
- Schedules are player-started, persisted, editable while running, and reject empty or single-stop lists.
- Automatic routes lock at departure and reserve/release sections stepwise.
- Graph changes trigger reroute when possible; otherwise the train stops and waits.
- Manual trains may ignore signals. Collisions derail all involved trains, pause schedules, and release routes.
- A derailed train keeps its current section locked until the train is reset and leaves the section. Other automatic
  trains reroute around the locked section when possible.
- Train damage and repair are not planned gameplay; a player tool resets derailed trains.

## Consequences

- Manual and automatic trains share the same movement, graph, persistence, and collision logic.
- Dispatch and signal behavior can be tested through the aggregate model without requiring full entity simulation.
- The timetable contract can be extended by addons through `StationOperation` and `DepartureCondition` without changing
  the base train model.
- Station and freight platform blocks become required later for timetable MVP, but this documentation-only change does not
  implement them.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
