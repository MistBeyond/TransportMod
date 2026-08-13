# Dispatch

Status: `contract` (dispatch rules; Graph + Dispatch runtime contract accepted)

Parent overview: `docs/roadmap/rail.md`

## Summary

The dispatch system manages timetables, route planning, reservations, and conflict handling while keeping automatic
and manual driving compatible. Signal state semantics live in `docs/roadmap/rail/sections.md`.

## Confirmed Directions

- Trains run from timetable and stop plans plus automatic pathfinding over the track graph.
- Pathfinding prefers the shortest path and applies a distance penalty for non-target stations.
- Routes are locked at departure, but sections are reserved and released step by step.
- Automatic schedules use explicit `ONE_WAY` or `LOOP` types and ordered generic stops.
- Schedules are player-started, persisted, editable while running, and reject empty or single-stop lists.
- The MVP default arrival behavior is stop at the stop node, then depart to the next stop.
- When a red signal or reserved section blocks the route, an automatic train stops and waits.
- When the graph changes, an automatic train reroutes when possible; otherwise it stops and waits.
- Manual trains may ignore signals and can collide with automatic trains. Collisions derail all involved trains.
- A derailed train keeps its current section locked until the train is reset and leaves that section.
- Other automatic trains reroute around a locked section when possible; otherwise they stop and wait.
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
- Deadlock avoidance beyond stop-and-wait behavior.
