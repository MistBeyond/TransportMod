# Sections

Status: `contract` (section and signal rules; Graph + Dispatch runtime contract accepted)

Parent overview: `docs/roadmap/rail.md`

## Summary

Rail sections are the pieces of connected track that signals divide and trains reserve while moving.

## Confirmed Directions

- The whole connected track graph forms one default rail section.
- Signals split that default section into smaller sections at their placement points.
- Signals can be placed in any `World Grid` cell containing track. Separation and merge nodes are preferred for
  snapping; crossings are not included in that preference.
- A signal placed mid-straight splits the section at that placement point, but does not create a new track graph node
  and does not change track geometry.
- Block signals and path signals use Satisfactory-like semantics.
- Block signal and path signal state semantics are owned by this document.
- Block signals and path signals are separate placeable signal items; the player chooses which type to place. A path
  signal may be placed on any cell of an approach chain whose facing side eventually reaches a routing node: from its
  own cell it reserves through the junction to the chosen exit, and it is not placed on the exit side. The guarded
  junction is the nearest routing node in the facing direction, which is unique because a chain has no other branch.
- At most one signal may be placed per cell per facing direction; a bidirectional cell may hold two signals facing
  opposite directions. Adjacent cells are not distance-constrained, so zero-length sections cannot arise.
- A signal's facing direction at placement is the player's look direction. Re-facing a signal is done only by breaking
  and replacing it; there is no in-place rotation interaction.
- Signal aspects are limited to `RED` and `GREEN`; a misconfigured signal additionally shows an `ERROR` indicator and
  behaves as `RED`.
- Placement is permissive: signals are not rejected for misconfiguration. A signal shows the `ERROR` indicator and
  behaves as `RED` when its facing side has no track, or (path signals) its facing side never reaches a routing node.
  Repositioning or refacing the signal restores its normal indication. `ERROR` is a configuration/health indicator, not
  a third aspect.
- The `ERROR` indicator is rendered as an exclamation overlay on the signal block's side and is also marked in the F3
  debug overlay.
- A path signal placed on a plain straight whose facing side never reaches a routing node is allowed but stays in the
  `ERROR` state until it is placed on a valid approach.
- Complex signal aspects, additional signal types, and custom signal states are addon extension points.
- A block signal is directional and protects the next section in its facing direction.
- A path signal is directional and, at a junction entrance, reserves the full conflict path through the junction to the
  chosen exit. The reservation is released after the train exits.
- Two paths through a junction conflict when they share any track segment or cell, or cross at the same routing-node
  cell at the same height; different-height crossings never conflict. A conflict path is such a non-concurrently
  reservable path; consequently only fully disjoint paths (different entry, different exit, no shared cell) may be
  traversed simultaneously, and merging or splitting paths that share an approach or exit segment conflict.
- A path signal is evaluated only for automatic trains, on the route locked at departure. It MAY display `GREEN` only
  when the required reservation for the train's locked route through the junction can be granted atomically; otherwise
  it displays `RED` (the route cannot be reserved because of occupancy or reservation under the junction conflict
  rules).
- Trains whose paths do not conflict MAY traverse a junction simultaneously; a given conflict path admits one train at
  a time.
- When multiple automatic trains compete for the same conflict path, the reservation grant follows first-come-first-
  served order by request arrival; losing trains stop, wait, and retry per the runtime-contract wake-up rules.
- Once a route reservation through the junction is granted, the related entrance signals turn `RED` until the train's
  tail exits the junction, defined as the set of cells the train's body occupies no longer intersecting the conflict
  path; the full conflict-path reservation is then released as a whole (per-segment progressive release is a future
  optimization).
- Manual trains are not signal-constrained; see the runtime contract.
- Signal state is derived from dispatch reservations, manual train presence, and derailment section locks.
- Signals have a direction; bidirectional track needs signals in both directions.
- The F3 debug overlay visualizes sections (colored per-section cell boxes with section ids) and signals (markers
  with facing-direction arrows); sections split by signals show their boundary count in the label. Section color
  preview during signal placement is planned.
- Block and path signals use distinct models and colors so the two types are distinguishable at a glance.
- The F3 debug overlay renders path signals with a dedicated marker (shape/label) and colors signal markers by their
  current aspect (`RED`/`GREEN`) or `ERROR` state; highlighting the reserved conflict path is not part of this
  milestone.
- Signals and their reservations follow the existing graph-change and derailment contract: removing track invalidates
  the affected reservations through reroute-on-change, and a train stalled inside a junction keeps its reservation
  until its tail exits or the player resolves the situation. No separate invalidation mechanism is required.
- Track and signal edits mark graph cache dirty. Validation is deferred until a train or player is nearby; a mismatch
  rebuilds the affected connected component.
- `World Grid` is an abstract term for the interior of each Minecraft block cell. It is not a coordinate point, block
  edge, or vertex, and it is not a code type or API type.
- Signals and rail sections use `World Grid` cell-interior placement semantics.

## Scope

- Default connected sections.
- Signal-based section splitting.
- Block signals and path signals.
- Signal direction.
- Section visualization.
- Deferred graph validation and connected-component rebuild.
- `World Grid` cell-interior placement concept.

## Open Questions

- (none)
