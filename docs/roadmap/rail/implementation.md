# Implementation

Status: `planned` (placeholder; content pending)

Parent overview: `docs/roadmap/rail.md`

## Summary

This document records the railway implementation direction, focusing on abstracting track networks, trains, and dispatch logic into testable server-side models.

## Scope

- Server-authoritative models and client synchronization boundaries.
- Track network and rail section abstraction.
- Train entity presentation and the server aggregate model.
- Dispatch logic layer and route selection.
- KubeJS event, binding, and type wrapper integration direction.
- Performance, testing, and multiplayer consistency.

## Open Questions

- How the track network is abstracted into testable logic.
- How the train entity and aggregate model relationship is settled.
- Whether the dispatch layer is independent of Minecraft block implementations.
- At what granularity KubeJS integration points are exposed to scripts.
