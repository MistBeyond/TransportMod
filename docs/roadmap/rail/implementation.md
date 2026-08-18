# Implementation

Status: `contract` (implementation notes; Graph + Dispatch runtime contract accepted)

Parent overview: `docs/roadmap/rail.md`

## Summary

This document records the railway implementation direction for the Graph + Dispatch MVP. The binding behavior contract is
`docs/roadmap/rail/runtime-contract.md`; architecture decisions are recorded in `docs/decisions/README.md`.

## Confirmed Directions

- `api.rail` contains service interfaces, read-only views, ID/request/result records, and stable public enums.
- `core.rail` contains `RailNetworkManager`, rail gameplay/business logic, public rail entry points, train aggregates,
  dispatch implementation, and internal domain records such as graph nodes, edges, and sections.
- `internal.rail` implements graph cache, train aggregate, timetable, and dispatch persistence.
- `api.rail.graph.TrackGraphSource` adapts world cells to track placements. `core.rail` owns graph collection,
  connectivity assembly, reachability, validation, and connected-component rebuilds; content packages only map block
  states to that source.
- `api.rail.graph.TrackCellData` is the read-only track cell contract. Content packages map BlockState and block entity
  data into it.
- The track block uses one block ID with a `direction` property holding one of the four track axes (N–S, E–W, NE–SW,
  NW–SE); the property stores one axis value, not eight one-way directions. Simple cells have no block entity; complex
  cells with crossings, curves, curve ramps, or signals use a block entity.
- Datagen generates simple track models; complex cells render through the baked-model route: a custom unbaked model
  resolved per block entity from `TrackCellData` through `ModelData`, merging complex-cell geometry into the chunk mesh
  (see `docs/decisions/0006-complex-track-cell-rendering.md`).
- Collision shapes are generated from `TrackCellData` as strips centered on the track axis that cover both rails and
  the area between them (24 px gauge plus both rail half-widths, i.e. 26 px); they are not clipped to the owning block's
  16x16 bounds and may overflow into neighboring cells. Complex cell collision is the union of the cell's rail collision
  shapes.
- Track cells occupy one block slot and do not support waterlogging. Physical occupancy is decoupled from collision
  shapes. Visual overflow does not create neighboring occupancy, collision, or graph connections; collision strip
  overflow does not create neighboring occupancy or graph connections.
- Train entities ignore track collision and use entity AABBs against ordinary blocks and other entities.
- The server owns a track graph model with nodes, edges, sections, signal boundaries, and reservation management.
- Curve sample-point connections create track graph nodes and may split the original curve edge.
- Diagonal 45 straight track is treated as ordinary straight track in node, edge, path, and reservation models.
- Track, signal, and switch edits mark graph cache dirty; validation is deferred until a train or player is nearby.
- Validation covers local tracks plus neighboring sections. A mismatch rebuilds the affected connected component.
- Abstract interfaces are planned for pathfinding, route locking, stepwise reservation, timetable behavior, and signal
  state queries.
- All trains use `core.rail.RailTrainAggregate`; manual and automatic are `RailControlMode` values.
- Timetable stops reference `RailStationId`; station and freight platform block implementation is required later for the
  timetable MVP.
- The F3 debug overlay (rail sections with colors and labels, plus signal markers with direction arrows) is
  implemented. Section color preview for signal placement, performance work, and further tests are planned.
- KubeJS integration should expose read access through immutable `RailNetworkSnapshot`, may use `core.rail`, and logs rare
  direct `internal.rail` exceptions.

## Scope

- Server-authoritative track graph owned per `ServerLevel`.
- Track nodes, edges, sections, and signal boundaries.
- Track cell storage, rendering, collision, and occupancy.
- Pathfinding, timetable, and reservation behavior.
- Deferred graph validation and connected-component rebuild.
- `api.rail`, `core.rail`, and `internal.rail` package responsibilities.
- Train aggregate, manual/automatic control modes, and collision/derailment state.
- KubeJS read-only snapshot access.

## Open Questions

- At what granularity KubeJS integration points are exposed to scripts.
- How large connected components should be tested and profiled.
