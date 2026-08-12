# Rail

Status: `planned` (overview and index; detailed specs pending)

Roadmap node: Node 1 Railway

## Summary

Railway is the first complete transport mode, focused on freight while allowing players to control trains. The overall
design follows Satisfactory and is composed of stations, tracks, trains, and a railway dispatch system.

## Design Overview

- Tracks use real-world rail models and widths as design references; exact gauge and block scale are still open.
- Tracks are represented by a server-side track graph for logic plus world blocks and models for presentation.
- `World Grid` is an abstract term for the interior of each Minecraft block cell. It is not a code type or API type.
  Full definition is inlined in `docs/roadmap/rail/tracks.md` and `docs/roadmap/rail/sections.md`.
- Track placement supports two modes: enhanced block placement and Satisfactory-like ghost preview placement.
- The whole connected track graph forms a default rail section; signals split it into smaller sections. The dispatch
  system is rooted in tracks and sections.
- Dispatch uses block signals, path signals, and autonomous train scheduling. The deeper logic abstraction is discussed
  in `docs/roadmap/rail/implementation.md`.
- Signals split connected track into rail sections. Automatic trains use timetable-based pathfinding, lock routes at
  departure, and reserve sections step by step.
- Manual trains can ignore signals and may collide with automatic trains, causing damage and requiring repair.
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
- Station form remains open, including single-block, multi-block, track-mounted, and modular stations. Loading and
  unloading belong to the station system.

## Subsystems

- Tracks: track models, scale, placement, connectivity, and rail sections.
- Sections: default connected rail sections and signal-based section splitting.
- Stations: station form, stopping, and loading/unloading.
- Trains: train composition, locomotive, freight underframe, replaceable cargo bodies, and player interaction.
- Dispatch: rail sections, signals, path signals, and train scheduling.
- Implementation: server authority, track network abstraction, train aggregate model, dispatch logic layer, and KubeJS
  integration.

## Confirmed Directions

- The railway system is broadly inspired by Satisfactory.
- Tracks use a track graph for logic and world presentation for gameplay.
- The whole connected track graph forms one default rail section; signals split it into smaller sections.
- Block signals and path signals use Satisfactory-like semantics.
- Automatic trains use timetable-based pathfinding, shortest path with station penalty, locked route, and stepwise
  reservation.
- Manual trains can ignore signals and may collide, causing damage and requiring repair.
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

## Open Questions

- Exact signal placement, section splitting, and visualization rules.
- Exact pathfinding, reservation, and deadlock handling algorithms.
- How manual collisions and repair flow work in detail.
- Which station form will be used.
- How real-world track width scales to Minecraft block dimensions.
- How freight underframes and replaceable cargo bodies connect and swap.
- How manual driving and autonomous dispatch coexist.
- Where the abstraction boundaries sit for the track network, dispatch logic, and server model.

## Subdocument Index

- `docs/roadmap/rail/tracks.md`
- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/stations.md`
- `docs/roadmap/rail/trains.md`
- `docs/roadmap/rail/dispatch.md`
- `docs/roadmap/rail/implementation.md`
