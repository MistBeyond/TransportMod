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
- Player-visible signal state is limited to `RED` and `GREEN`.
- Complex signal aspects, additional signal types, and custom signal states are addon extension points.
- A block signal is directional and protects the next section in its facing direction.
- A path signal is directional and, at a junction entrance, reserves the full conflict path through the junction to the
  chosen exit. The reservation is released after the train exits.
- Signal state is derived from dispatch reservations, manual train presence, and derailment section locks.
- Signals have a direction; bidirectional track needs signals in both directions.
- The F3 debug overlay visualizes sections (colored per-section cell boxes with section ids) and signals (markers
  with facing-direction arrows); sections split by signals show their boundary count in the label. Section color
  preview during signal placement is planned.
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

- How signals are visualized and debugged.
