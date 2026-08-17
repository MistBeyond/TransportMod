# Rail

Status: `contract` (overview and index; Graph + Dispatch runtime contract accepted)

Roadmap node: Node 1 Railway

## Summary

Railway is the first complete transport mode, focused on freight while allowing players to control trains. The overall
design follows Satisfactory and is composed of stations, tracks, trains, and a railway dispatch system.

The binding runtime contract is `docs/roadmap/rail/runtime-contract.md`. Architecture decisions are recorded in
`docs/decisions/README.md`.

## Design Overview

- Tracks use real-world rail models and widths as design references; exact gauge and block scale are still open.
- Tracks are represented by a server-side track graph for logic plus world blocks and models for presentation.
- Track cells use `api.rail.graph.TrackCellData` with one block ID. Simple cells use BlockState direction; complex cells
  use a block entity and render through the baked-model route (custom unbaked model + per-block-entity `ModelData`).
- `World Grid` is an abstract term for the interior of each Minecraft block cell. It is not a code type or API type.
  Full definition is inlined in `docs/roadmap/rail/tracks.md` and `docs/roadmap/rail/sections.md`.
- Track placement supports two modes: enhanced block placement and Satisfactory-like ghost preview placement.
- The whole connected track graph forms a default rail section; signals split it into smaller sections. The dispatch
  system is rooted in tracks and sections.
- Dispatch uses block signals, path signals, and autonomous train scheduling. The deeper logic abstraction is discussed
  in `docs/roadmap/rail/implementation.md`.
- Signals split connected track into rail sections. Automatic trains use timetable-based pathfinding, lock routes at
  departure, and reserve sections step by step.
- Manual trains can ignore signals and may collide with automatic trains. Collisions derail all involved trains; the
  current section stays locked until the derailed train is reset and leaves it.
- Same-level intersections can form routing nodes; different-height crossings do not connect.
- Curves and switches use semi-free placement via sampled curve connection points. Diagonal 45 straight track is a
  first-class straight track type with the same status as ordinary straight track; details live in
  `docs/roadmap/rail/tracks.md`.
- Tracks are bidirectional by default; directional signals define one-way movement.
- Trains are split into separate car bodies. A train contains a locomotive and a freight portion made of a freight
  underframe plus a replaceable cargo body.
- Replaceable cargo bodies support item or fluid storage.
- Players can sit in the locomotive to drive and can stand on any train part, moving with the train.
- The implementation direction uses a hybrid model: separate physical entities for presentation plus a server-side
  aggregate train model.
- All trains use the same server-side aggregate; manual and automatic are control modes, not separate train models.
- Stations are one-way timetable stops. A station may have a behind-chain of freight platforms or other stations.
  Loading and unloading occur only on freight platforms.

## Subsystems

- Tracks: track models, scale, placement, connectivity, and rail sections.
- Sections: default connected rail sections and signal-based section splitting.
- Stations: station stop direction, freight platform chain, and loading/unloading.
- Trains: train composition, locomotive, freight underframe, replaceable cargo bodies, and player interaction.
- Dispatch: rail sections, signals, path signals, and train scheduling.
- Implementation: server authority, track network abstraction, train aggregate model, dispatch logic layer, and KubeJS
  integration.

## Confirmed Directions

- The railway system is broadly inspired by Satisfactory.
- Tracks use a track graph for logic and world presentation for gameplay.
- The whole connected track graph forms one default rail section; signals split it into smaller sections.
- Block signals and path signals use Satisfactory-like semantics.
- Player-visible signal state is limited to `RED` and `GREEN`; complex signal states are addon extensions.
- Automatic trains use timetable-based pathfinding, shortest path with station penalty, locked route, and stepwise
  reservation.
- Manual trains can ignore signals; collisions derail all involved trains and lock the occupied section until reset and
  leave.
- Same-level intersections can form routing nodes; different-height crossings do not connect.
- Curves and switches use semi-free placement via sampled curve connection points. Diagonal 45 straight track is a
  first-class straight track type with the same status as ordinary straight track; details live in
  `docs/roadmap/rail/tracks.md`.
- Tracks are bidirectional by default; directional signals define one-way movement.
- `World Grid` means the interior of each Minecraft block cell and applies to tracks, nodes, signals, and rail sections.
- "Freight car" is an abstract term; the actual design is a freight underframe plus a replaceable cargo body.
- Replaceable cargo bodies can carry items or fluids. Cargo body types and replacement rules are discussed in
  `docs/roadmap/rail/trains.md`.
- Connected or intersecting tracks form rail sections.
- Dispatch uses block signals, path signals, and autonomous train scheduling.
- Trains use separate entity presentation while the server keeps an aggregate train model.
- All trains use one aggregate model; manual and automatic are control modes.
- Automatic schedules use explicit `ONE_WAY` or `LOOP` types with ordered generic stops.
- Timetable stops reference `RailStationId`; station behind-chain freight platforms perform binary `LOAD`/`UNLOAD`.
- The main departure condition is `OPERATION_COMPLETE`; `StationOperation` and `DepartureCondition` are addon extension
  points.
- Track edits mark graph cache dirty; validation is deferred until a train or player is nearby and rebuilds the affected
  connected component.
- Track cell collision is generated from `TrackCellData` as gauge-wide strips covering both rails and the area between
  them (24 px gauge plus both rail widths, i.e. 26 px), not clipped to the owning block; complex cell collision is the
  union of the cell's rail collision shapes. Occupancy stays one block slot per cell and is decoupled from collision
  shapes. Visual overflow does not occupy neighboring cells.
- Signal semantics, timetable behavior, collision/derailment, and persistence follow
  `docs/roadmap/rail/runtime-contract.md`.

## Open Questions

- Signal visualization and debug presentation rules.
- Deadlock avoidance beyond stop-and-wait behavior.
- Exact station block placement and visual model.
- How real-world track width scales to Minecraft block dimensions.
- How freight underframes and replaceable cargo bodies connect and swap.

## Subdocument Index

- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/tracks.md`
- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/stations.md`
- `docs/roadmap/rail/trains.md`
- `docs/roadmap/rail/dispatch.md`
- `docs/roadmap/rail/implementation.md`
