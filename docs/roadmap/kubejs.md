# KubeJS

Status: `next` (placeholder; content pending)

Roadmap node: parallel track from Node 0

## Summary

KubeJS integration advances alongside each transport mode. It exposes content registration, property definitions,
events, and read-only state access.

## Capability Scope

### Content Registration

- Blocks.
- Items.
- Trains and vehicles.
- Freight underframes.
- Replaceable cargo bodies.
- Stations.
- Signals.
- Track types, including straight, diagonal 45, curves, and curve ramps.

### Property Definition

- Speed and acceleration.
- Capacity and cargo type.
- Model, texture, and sound.
- Other definition-level transport properties exposed through `api`.

### Events

- Vehicle creation and destruction.
- Station arrival and departure.
- Loading and unloading.
- Signal state changes.
- Track graph changes.
- Collision and fault events.

### Read-Only State

- Track graph.
- Rail sections.
- Signals.
- Trains and vehicles.
- Stations.
- Freight state.
- Timetable state.

### Data Extensions

- Recipes.
- Tags.
- Language files.

### Not Allowed

- Mutating the live track graph, sections, or dispatch state.
- Spawning or removing trains.
- Modifying timetables.
- Directly writing to internal implementation details.

## Gameplay Scope

- Scripts can read transport state on demand.
- Scripts can respond to key state changes.
- Content registration is allowed by scope, but exact KubeJS builders may roll out after the base integration is
  stable.

## Implementation Direction

- All KubeJS code lives under `integration.kubejs`; no top-level `kubejs` package is created.
- The plugin entry is declared through `src/main/resources/kubejs.plugins.txt`.
- TransportMod implements a `KubeJSPlugin` that registers bindings, event groups, type wrappers, and content builders.
- KubeJS defaults to `api`, may depend on `core` without registration, and logs rare direct `internal` exceptions in
  code comments and `docs/decisions/README.md`.
- Scripts can read rail, truck, station, and transport state through public contracts first.

## Integration Points

- Foundation: plugin skeleton, plugin discovery, base bindings, event groups, and builders.
- Rail: train, track, station, signal, and loading/unloading events and bindings.
- Truck: capacity and road transport events and bindings.
- Drone: pending gameplay confirmation.
- Water: pending gameplay confirmation.

## Open Questions

- Which state changes emit events and which are queried through bindings.
- How custom content builders are exposed to scripts.
- Whether the KubeJS dependency and version enter the formal build configuration.
