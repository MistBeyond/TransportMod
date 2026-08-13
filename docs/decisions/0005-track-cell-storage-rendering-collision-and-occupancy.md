# ADR 0005: Track Cell Storage, Rendering, Collision, and Occupancy

Status: Accepted

Date: 2026-08-14

## Context

The current `TestTrackBlock` uses a BlockState axis and a full 16x16x2 collision box. That is too limited for crossings,
curves, curve ramps, signals, and multiple placements inside one `World Grid` cell. The rail system needs a storage,
rendering, collision, and occupancy model that can represent complex track cells without inventing a new world type.

## Decision

- Use one track block ID `rail_track_cell` with an 8-direction `direction` BlockState property.
- Treat a cell as simple when it contains exactly one straight or diagonal 45 placement and no signal. A simple cell has
  no block entity.
- Treat a cell as complex when it contains multiple placements, a curve, a curve ramp, a crossing, or a signal. A complex
  cell has a block entity.
- Add `api.rail.graph.TrackCellData` as a read-only view containing `Set<TrackPlacement>` and signal placement
  information.
- Make `TrackGraphSource` read `TrackCellData` instead of directly reading BlockState axis. The world adapter checks the
  block entity first and falls back to the BlockState direction when no block entity exists.
- Persist and synchronize complex block entities through standard NBT and update tags. Do not introduce custom packets.
- Add `CURVE` and `CURVE_RAMP` to `TrackType` as future track types; exact curve geometry is defined later.
- Generate simple track models through datagen for all 8 directions. Render complex cells with a block entity renderer
  that reads `TrackCellData`.
- Allow visual models to overflow neighboring cells. Visual overflow does not affect graph logic, collision, or
  occupancy.
- Generate collision shapes from `TrackCellData`; clip them to the owning block's 16x16 bounds. Keep the current
  approximate height of 2 pixels (`0.125` blocks).
- Represent complex cell collision as the union of all rail collision shapes in that cell.
- Make trains ignore track collision shapes. Train entities collide with ordinary world blocks, other trains, and other
  entities through entity AABBs.
- Give each track cell exactly one `World Grid` block slot. Physical occupancy inside that slot equals the collision
  shape.
- Do not support waterlogged track cells in the main mod.
- Allow visual overflow to pass through any neighboring block. It does not occupy neighboring slots and does not create
  neighboring collision.
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
