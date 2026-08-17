# ADR 0006: Complex Track Cell Rendering — Baked-Model Route

Status: Accepted

Date: 2026-08-16

## Context

Complex track cells (crossings, curves, curve ramps, signals) hold multiple `TrackPlacement` entries inside one block
entity. Their geometry cannot be enumerated statically: curve radius, arc angle, and connection points are unbounded,
so datagen cannot pre-generate every possible curve JSON. The rail system needs a rendering path that computes
geometry at runtime from `TrackCellData` and shares the rail geometry parameters (gauge 24 px, rail 2 x 2.4 px, caps
3 x 0.5 px, sleepers 33 x 1 x 3 px) with the datagen simple-track generator, otherwise straight and curved track
visually drift apart. The earlier architecture text ("the block entity renderer renders complex cells") was written
before this analysis; this ADR replaces that rendering contract.

## Decision

- Render complex cells through the baked-model route: a custom unbaked model (26.1 `UnbakedModelLoader` /
  `UnbakedModelExtension`) resolved per block entity from `TrackCellData` via per-BE `ModelData`
  (`net.neoforged.neoforge.model.data.ModelDataManager`). Complex-cell geometry is merged into the chunk mesh and
  inherits frustum culling, occlusion culling, and batching.
- Do not use a block entity renderer (BER) for complex track cells. Each BER cell costs one draw call per frame;
  the baked-model route has no per-cell per-frame draw cost. (A BER implementation was considered sufficient for the
  MVP and remains acceptable as a fallback if the baked-model route proves infeasible, but it is not the chosen
  design.)
- Move the shared rail geometry parameters out of the datagen source set into `main` (proposed
  `com.mistbeyond.transport.client.rail.model`), because `datagen` depends on `main` and `main` must not depend on
  `datagen`. The datagen `TrackModelGenerator` then imports them. After extraction, re-run datagen and verify the
  generated `track_diagonal.json` is byte-identical to the pre-migration output.
- Geometry generation (curve sampling, rail sweeping, sleeper placement) is pure math in `main` and MUST be unit
  tested (symmetry, seam coincidence, sleeper spacing assertions).
- Adjacent cells' rail end faces MUST coincide exactly back-to-back (opposite normals, backface culling renders one
  face — no z-fighting). Neither gaps nor overlapping coplanar faces are acceptable.
- Sleepers MUST be placed by global arc length (s = k * d), not by cell-local origin, because the cell pitch is not
  an integer multiple of the sleeper spacing; `TrackPlacement`/curve data carries the global arc-length parameter.
- Simple cells (straight and diagonal 45) are unaffected: datagen models remain, and the hand-written straight model
  `block/track` stays untouched (parameterizing the straight track is explicitly out of scope).
- Geometry is static: generate once, cache, draw per frame. Rebuilding geometry per frame is forbidden.
- Track models use cutout rendering like vanilla rails; the block must declare the corresponding RenderType.

## Consequences

- Complex-cell geometry becomes part of the chunk mesh; no per-cell per-frame draw call overhead.
- `architecture.md`, `docs/roadmap/rail/tracks.md` (Rendering), `docs/roadmap/rail/implementation.md`, and
  `docs/roadmap/rail/runtime-contract.md` (acceptance scenario 20) are updated to the baked-model wording; the
  earlier "block entity renderer" wording is superseded by this ADR.
- The shared geometry parameters move from the datagen source set to `main`; `TrackModelGenerator` imports them.
- The straight track model remains hand-written and untouched; only the diagonal and future curve geometry is
  generated.

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/tracks.md`
- `docs/roadmap/rail/implementation.md`
- `docs/decisions/0005-track-cell-storage-rendering-collision-and-occupancy.md`
