# Implementation

Status: `planned` (concept notes; detailed specs pending)

Parent overview: `docs/roadmap/rail.md`

## Summary

This document records the railway implementation direction, focusing on abstracting track networks, trains, and dispatch logic into testable server-side models.

## Confirmed Directions

- `api.rail` contains rail contracts.
- `core.rail` contains rail gameplay/business logic, public rail entry points, and `api` implementations.
- `internal.rail` is a reserved implementation-detail location; its exact contents and boundary are defined later by
  code practice.
- The server owns a track graph model with nodes, edges, sections, signal boundaries, and reservation management.
- Curve sample-point connections create track graph nodes and may split the original curve edge.
- Diagonal 45 straight track is treated as ordinary straight track in node, edge, path, and reservation models.
- Track, signal, and switch changes update the graph and sections in real time.
- Abstract interfaces are planned for pathfinding, route locking, stepwise reservation, and signal state queries.
- Section color preview, debug views, performance work, and tests are planned.
- KubeJS integration should expose read access through `api.rail`, may use `core.rail`, and logs rare direct
  `internal.rail` exceptions.

## Scope

- Server-authoritative track graph.
- Track nodes, edges, sections, and signal boundaries.
- Pathfinding and reservation abstraction.
- Real-time graph updates.
- `api.rail`, `core.rail`, and `internal.rail` package responsibilities.
- KubeJS event, binding, and type wrapper integration direction.
- Performance, testing, and multiplayer consistency.

## Open Questions

- How the track network is abstracted into testable logic.
- How the train entity and aggregate model relationship is settled.
- Whether the dispatch layer is independent of Minecraft block implementations.
- At what granularity KubeJS integration points are exposed to scripts.
