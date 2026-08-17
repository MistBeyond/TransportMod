# ADR 0005: Track Cell Storage, Rendering, Collision, and Occupancy

Status: Accepted

Date: 2026-08-14

Revised: 2026-08-16 — Collision shapes changed from 16x16-clipped narrow rails to gauge-wide strips (24 px gauge plus
both rail widths, i.e. 26 px) centered on the track axis that may overflow neighboring cells; physical occupancy is
decoupled from collision shapes. See the decision bullets below.

Revised: 2026-08-16 — Simple straight/diagonal 45 cells are bidirectional: the graph adapter emits the `direction`
placement and its opposite, so a line of same-facing simple cells forms a connected bidirectional track graph and
manual driving works. The BlockState still stores one `direction` value; crossings and corners still require complex
cells with multiple placements. See the Track Cell Storage bullet below.

Revised: 2026-08-16 — The simple-cell BlockState `direction` property stores one of the four track axes (N–S, E–W,
NE–SW, NW–SE) instead of eight one-way directions. Since a simple cell is a bidirectional segment, each pair of
opposite directions (N/S, E/W, NE/SW, NW/SE) is equivalent; the graph adapter expands the stored axis into both
directional placements. Signal direction still uses the full 8-direction `GridDirection`; only simple-cell storage
collapses to four axes. See the decision bullets below.

## Context

The current `TestTrackBlock` uses a BlockState axis and a full 16x16x2 collision box. That is too limited for crossings,
curves, curve ramps, signals, and multiple placements inside one `World Grid` cell. The rail system needs a storage,
rendering, collision, and occupancy model that can represent complex track cells without inventing a new world type.

## Decision

- Use one track block ID `rail_track_cell` with a `direction` BlockState property holding one of the four track axes
  (N–S, E–W, NE–SW, NW–SE); the property stores one axis value, not eight one-way directions.
- Treat a cell as simple when it represents exactly one straight or diagonal 45 segment (one axis) and no signal. The
  BlockState `direction` property stores one axis value; the graph adapter interprets a simple cell as a bidirectional
  segment and emits both directional placements of the stored axis. A simple cell has no block entity.
- Treat a cell as complex when it contains multiple placements, a curve, a curve ramp, a crossing, or a signal. A complex
  cell has a block entity.
- Add `api.rail.graph.TrackCellData` as a read-only view containing `Set<TrackPlacement>` and signal placement
  information.
- Make `TrackGraphSource` read `TrackCellData` instead of directly reading BlockState axis. The world adapter checks the
  block entity first and falls back to the BlockState direction when no block entity exists.
- Persist and synchronize complex block entities through standard NBT and update tags. Do not introduce custom packets.
- Add `CURVE` and `CURVE_RAMP` to `TrackType` as future track types; exact curve geometry is defined later.
- Generate simple track models through datagen for all 4 axis values. Render complex cells through the baked-model
  route: a custom unbaked model resolved per block entity from `TrackCellData` through `ModelData` (see
  `docs/decisions/0006-complex-track-cell-rendering.md`).
- Allow visual models to overflow neighboring cells. Visual overflow does not affect graph logic, collision, or
  occupancy.
- Generate collision shapes from `TrackCellData` as strips centered on the track axis that cover both rails and the
  area between them. Strip width follows the full rail profile: the 24 px gauge (rail center distance, 1.5 blocks)
  plus both rail widths (2 px each), i.e. 26 px (1.625 blocks). Keep the current approximate height of 2 pixels
  (`0.125` blocks).
- Do not clip collision strips to the owning block's 16x16 bounds; they may overflow into neighboring cells.
- Represent complex cell collision as the union of all rail collision shapes in that cell.
- Make trains ignore track collision shapes. Train entities collide with ordinary world blocks, other trains, and other
  entities through entity AABBs.
- Give each track cell exactly one `World Grid` block slot. Physical occupancy is decoupled from collision shapes:
  collision strip overflow does not occupy neighboring slots.
- Do not support waterlogged track cells in the main mod.
- Allow visual overflow to pass through any neighboring block. It does not occupy neighboring slots and does not create
  neighboring collision. Collision strip overflow does not create neighboring occupancy or graph connections.
- Do not create graph connections from visual overlap. Connections come only from explicit `TrackPlacement` entries in
  `TrackCellData`.

## Consequences

- Complex track geometry can be stored and rendered without BlockState explosion.
- `core.rail` remains independent of concrete block and renderer implementations.
- Collision and occupancy have one shared source of truth: `TrackCellData`.
- The existing `TestTrackBlock` will be replaced when the implementation contract is implemented.
- Station and freight platform block entities remain a separate future ADR.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/tracks.md`
- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
