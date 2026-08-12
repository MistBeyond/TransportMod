# Dispatch

Status: `planned` (concept notes; detailed specs pending)

Parent overview: `docs/roadmap/rail.md`

## Summary

The dispatch system manages timetables, route planning, reservations, and conflict handling while keeping automatic
and manual driving compatible. Signal state semantics live in `docs/roadmap/rail/sections.md`.

## Confirmed Directions

- Trains run from timetable and stop plans plus automatic pathfinding over the track graph.
- Pathfinding prefers the shortest path and applies a distance penalty for non-target stations.
- Routes are locked at departure, but sections are reserved and released step by step.
- Manual trains may ignore signals and can collide with automatic trains, causing damage and requiring repair.
- If track ahead is removed, automatic trains stop and wait for repair or manual intervention.

## Scope

- Timetables and stop plans.
- Track graph pathfinding.
- Route locking and stepwise reservation.
- Manual and automatic driving coexistence.
- Collision, damage, and repair concepts.
- Signal semantics reference to `docs/roadmap/rail/sections.md`.

## Open Questions

- Path reservation and release rules.
- How dispatch priority and schedules are defined.
- How dispatch logic stays independent of concrete Minecraft blocks.
- How deadlock and manual-caused collisions are resolved in detail.
