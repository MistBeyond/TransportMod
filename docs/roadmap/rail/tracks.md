# Tracks

Status: `contract` (track rules; Graph + Dispatch runtime contract accepted)

Parent overview: `docs/roadmap/rail.md`

## Summary

Tracks are the foundation of the railway network. They provide train paths, rail sections, and the player-facing placement experience.

## Confirmed Directions

- Tracks use a server-side track graph for connectivity and pathfinding; world blocks and models provide presentation.
- Graph ownership, persistence, validation, and rebuild behavior follow `docs/roadmap/rail/runtime-contract.md`.
- Track edits mark saved graph cache dirty. Validation is deferred until a train or player is nearby; a mismatch rebuilds
  the affected connected component.
- Straight track, curves, switches, branches, and intersections are separate conceptual pieces.
- Curves and switches use semi-free placement.
- Curve ramps can be curved track segments. They can branch from or merge into a curve sample point.
- Curve sample points are curve endpoints plus fixed arc-length interval points. The exact interval is determined later
  during modeling.
- A curve ramp's other end must connect to another track, or terminate as a straight or diagonal 45 straight track free
  end.
- Connecting to a curve sample point creates a track graph node and may split the original curve edge.
- Diagonal 45 straight track is a first-class straight track type; see `Track Types`.
- `World Grid` is an abstract term for the interior of each Minecraft block cell. It is not a coordinate point, block
  edge, or vertex, and it is not a code type or API type.
- `World Grid` is the logical anchor cell for placement and snapping. Visual track models may overflow into neighboring
  cells without changing track graph, path, section, or collision logic.
- `World Grid` applies to tracks, nodes, signals, and rail sections as the shared placement/snapping reference.
- Same-level intersections can form routing nodes; different-height crossings do not connect.
- Tracks are bidirectional by default; directional signals define one-way movement.
- Real-world track width can inform visual design, but exact model scale is open.
- Track cells use `api.rail.graph.TrackCellData` as the read-only world adapter contract.
- The main track block uses one block ID with an 8-direction `direction` property. Simple cells have no block entity;
  complex cells have a block entity.
- `TrackType` includes `CURVE` and `CURVE_RAMP` for future curve geometry.

## Track Types

### Straight

- Connects adjacent `World Grid` cell centers along a cardinal direction.
- Length is one World Grid cell step.
- Participates in the track graph as an ordinary straight edge.
- Can connect to straight, diagonal 45, curves, and curve ramps at either end.

### Diagonal 45 Straight

- Connects adjacent `World Grid` cell centers diagonally.
- Direction is 45 degrees from cardinal directions.
- Path length is one World Grid cell diagonal.
- Participates in the track graph as an ordinary straight edge for nodes, edges, paths, sections, and reservations.
- Can connect to straight, diagonal 45, curves, and curve ramps at either end.

## Track Cell Storage

- A simple cell contains exactly one straight or diagonal 45 placement and no signal. It is stored by the BlockState
  `direction` property.
- A complex cell contains multiple placements, a curve, a curve ramp, a crossing, or a signal. It is stored by the track
  cell block entity.
- `TrackGraphSource` reads `TrackCellData`: it checks the block entity first and falls back to the BlockState direction
  when no block entity exists.
- Upgrading a simple cell to complex creates the block entity and preserves existing placements.
- Downgrading a complex cell to simple removes the block entity and writes the single remaining placement back to the
  BlockState direction.
- Complex block entities use standard NBT persistence and update tags.

## Rendering

- Datagen generates simple track block models for all 8 `direction` values.
- Complex cells use a block entity renderer that reads `TrackCellData`.
- Visual models may overflow neighboring cells. Visual overflow does not affect graph logic, collision, or occupancy.

## Collision and Occupancy

- `getCollisionShape` and `getShape` use generated track collision shapes instead of a full 16x16x2 box.
- Simple cells use fixed cached collision shapes per direction and track type.
- Complex cells use dynamic collision shapes generated from `TrackCellData`.
- Collision follows actual rail geometry clipped to the owning block's 16x16 bounds.
- Complex cell collision is the union of all rail collision shapes in that cell.
- Collision height stays at the current approximate track height of 2 pixels (`0.125` blocks).
- A track cell occupies exactly one `World Grid` block slot. Waterlogged track cells are not supported.
- Physical occupancy inside a cell equals its collision shape.
- Multiple placements may share one complex cell; occupancy is the union of their collision shapes.
- Visual overflow does not occupy neighboring slots and does not create neighboring collision.
- Visual overlap does not create graph connections; connections come only from explicit `TrackPlacement` entries.
- Trains ignore track collision shapes. Train entities collide with ordinary world blocks, other trains, and other
  entities through entity AABBs.

## Scope

- Track graph and world presentation relationship.
- Straight track, curves, switches, branches, and intersections.
- Node snapping and free placement concepts.
- Signal-defined one-way movement.
- Section formation from connected track.
- `World Grid` cell-interior placement concept.

## Open Questions

- How real-world gauge scales to Minecraft block dimensions.
- How placement interaction can be convenient and predictable.
- What exact curve sampling interval and curve geometry constraints apply.
- What slope rules apply.
- How this system relates to and differs from vanilla Minecraft rails.
