# Tracks

Status: `planned` (concept notes; detailed specs pending)

Parent overview: `docs/roadmap/rail.md`

## Summary

Tracks are the foundation of the railway network. They provide train paths, rail sections, and the player-facing placement experience.

## Confirmed Directions

- Tracks use a server-side track graph for connectivity and pathfinding; world blocks and models provide presentation.
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
