# Dispatch

Status: `contract` (dispatch rules; Graph + Dispatch runtime contract accepted)

Parent overview: `docs/roadmap/rail.md`

## Summary

The dispatch system manages timetables, route planning, reservations, and conflict handling while keeping automatic
and manual driving compatible. Signal state semantics live in `docs/roadmap/rail/sections.md`.

## Confirmed Directions

- Trains run from timetable and stop plans plus automatic pathfinding over the track graph.
- Pathfinding prefers the shortest path and applies a distance penalty for non-target stations.
- Routes are locked at departure; ordinary sections along a route are reserved and released step by step, while a
  junction's conflict path (guarded by a path signal) is held as a whole and released after the train's tail exits the
  junction. Signal semantics live in `docs/roadmap/rail/sections.md`.
- Automatic schedules use explicit `ONE_WAY` or `LOOP` types and ordered generic stops.
- Timetable stops reference `RailStationId`; they do not reference raw track graph nodes.
- Schedules are player-started, persisted, editable while running, and reject empty or single-stop lists.
- A train stops at a station only while facing the station's stop direction; reverse passage is allowed.
- All freight platforms in a station's behind-chain participate in the stop.
- The main departure condition is `OPERATION_COMPLETE`: the train departs after all participating freight platforms
  complete.
- A station without freight platforms stops the train and then departs.
- If a freight platform cannot complete its operation, the train waits.
- `StationOperation` and `DepartureCondition` are addon extension points; the main mod provides `LOAD`/`UNLOAD` and
  `OPERATION_COMPLETE`.
- When a red signal or reserved section blocks the route, an automatic train stops and waits, and retries when the
  blocking condition clears (reservation, occupancy, or signal-state change; see the runtime contract).
- When the graph changes, an automatic train reroutes when possible; otherwise it stops and waits.
- Manual trains may ignore signals and can collide with automatic trains. Collisions derail all involved trains.
- A derailed train keeps its current section locked until the train is reset and leaves that section.
- Other automatic trains reroute around a locked section when possible; otherwise they stop and wait.
- Automatic deadlock breaking is a future extension; cyclic deadlocks involving multiple junctions are outside the scope
  of automatic resolution and MUST be resolved through player actions (no dedicated tool is provided beyond the derail
  reset).
- Train damage and repair are not planned gameplay; a player tool resets derailed trains.

## Scope

- Timetables and stop plans.
- Track graph pathfinding.
- Route locking and stepwise reservation.
- Manual and automatic driving coexistence.
- Collision, derailment, section lock, and reset behavior.
- Signal semantics reference to `docs/roadmap/rail/sections.md`.

## Open Questions

- How dispatch priority and schedules are defined.
