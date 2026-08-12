# Sections

Status: `planned` (concept notes; detailed specs pending)

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
- Signals have a direction; bidirectional track needs signals in both directions.
- Section color preview is planned for signal placement and debug visualization.
- Track and signal changes update the graph and sections in real time.
- `World Grid` is an abstract term for the interior of each Minecraft block cell. It is not a coordinate point, block
  edge, or vertex, and it is not a code type or API type.
- Signals and rail sections use `World Grid` cell-interior placement semantics.

## Scope

- Default connected sections.
- Signal-based section splitting.
- Block signals and path signals.
- Signal direction.
- Section visualization.
- Real-time graph updates.
- `World Grid` cell-interior placement concept.

## Open Questions

- Exact section splitting rules.
- Exact block signal and path signal state semantics.
- How signals are visualized and debugged.
