# Railway Runtime Contract

Status: `accepted` (Graph + Dispatch MVP; binding for implementation)

Parent overview: `docs/roadmap/rail.md`

## Purpose

This document defines the minimum runtime contract for the railway Graph + Dispatch MVP. It is binding for future
implementation work. Existing rail concept notes remain useful for gameplay intent, but where this contract conflicts
with them, this document wins.

## Scope

- Server-authoritative track graph, rail sections, dispatch, and train aggregate state.
- Track, signal, and timetable behavior needed to run automatic and manual trains.
- Persistence, validation, threading, collision, and derailment behavior.

Out of scope for this MVP:

- Station and freight platform block implementations, cargo body storage, and station forms.
- Truck, drone, water transport, and KubeJS content registration.

Station and freight platform gameplay is contracted in this document and required later for the timetable MVP, but is not
implemented in this documentation-only change.

## Graph Ownership and Lifecycle

- `core.rail.RailNetworkManager` MUST own one rail network per `ServerLevel`.
- The manager MUST own the graph, dispatch state, train aggregates, dirty tracking, validation, and save/load
  coordination.
- The manager MUST NOT be a static `ConcurrentHashMap`.
- All writes MUST execute on the server main thread.
- Content packages MUST report world changes through `api.rail` or `core.rail` entry points; `core` MUST NOT depend on
  `block`, `item`, `entity`, or `client`.
- `World Grid` remains an abstract placement reference; it is not a code type or API type.

## Persistence and Validation

- World blocks are authoritative; the saved graph is a cache.
- `internal.rail` MUST implement graph cache, train aggregate, timetable, and dispatch persistence.
- On first load, the manager MUST NOT scan the whole world. It MUST build graph data lazily around nearby trains or
  players.
- Normal track or signal edits MUST mark the affected region dirty.
- Validation MUST run when a train or player is near the relevant loaded region.
- Validation scope MUST be local tracks plus neighboring sections; a mismatch MUST rebuild the affected connected
  component.
- Edits in unloaded chunks MUST be validated when the chunk is loaded; no global dirty queue is required.
- Saved state MUST include the graph cache, train aggregates, timetables, and dispatch state. Derived routes and
  reservations may be rebuilt after load.

## Threading and Snapshots

- Server main thread writes are the only accepted mutation path.
- `RailNetworkSnapshot` MUST be immutable.
- Reads for rendering, debug, KubeJS, and external integration MUST use immutable snapshots.
- The MVP MUST NOT use asynchronous graph mutation. Asynchronous work may produce immutable snapshots only when a later
  ADR explicitly allows it.

## Track Cell Storage, Rendering, Collision, and Occupancy

- `api.rail.graph.TrackCellData` MUST be the read-only view of one track cell's placements and signal placement
  information.
- The main track block MUST use one block ID with an 8-direction `direction` BlockState property.
- A simple cell MUST contain exactly one straight or diagonal 45 placement and no signal. A simple cell MUST NOT have a
  block entity.
- A complex cell MUST contain multiple placements, a curve, a curve ramp, a crossing, or a signal. A complex cell MUST
  have a block entity.
- `TrackGraphSource` MUST read `TrackCellData`; it MUST check the block entity first and fall back to the BlockState
  direction when no block entity exists.
- Simple-to-complex upgrades MUST create the block entity and preserve existing placements. Complex-to-simple downgrades
  MUST remove the block entity and write the remaining placement back to the BlockState.
- Complex block entities MUST use standard NBT persistence and update tags. Custom packets MUST NOT be used.
- `TrackType` MUST include `CURVE` and `CURVE_RAMP` for future track geometry.
- Datagen MUST generate simple track models for all 8 `direction` values. Complex cells MUST use a block entity renderer
  that reads `TrackCellData`.
- Visual models MAY overflow neighboring cells. Visual overflow MUST NOT affect graph logic, collision, or occupancy.
- Collision shapes MUST be generated from `TrackCellData` and clipped to the owning block's 16x16 bounds.
- Complex cell collision MUST be the union of all rail collision shapes in that cell.
- Collision height MUST remain at the current approximate track height of 2 pixels (`0.125` blocks).
- Trains MUST ignore track collision shapes. Train entities MUST collide with ordinary world blocks, other trains, and
  other entities through entity AABBs.
- Each track cell MUST occupy exactly one `World Grid` block slot. Physical occupancy inside that slot MUST equal its
  collision shape.
- Track cells MUST NOT support waterlogged states in the main mod.
- Visual overflow MUST NOT occupy neighboring slots or create neighboring collision.
- Visual overlap MUST NOT create graph connections; connections MUST come only from explicit `TrackPlacement` entries.

## Train Aggregate and Control Modes

- All trains use the same `core.rail.RailTrainAggregate` model.
- Manual and automatic are `RailControlMode` values, not separate train classes.
- Manual mode is driven by player input; automatic mode is driven by `RailTrainSchedule`.
- Switching to manual MUST pause the automatic schedule and release its route/reservations.
- The entity is presentation only; it MUST NOT own route planning, graph state, or dispatch decisions.
- Derailment state MUST belong to the train aggregate.

## Timetable

- `RailTrainSchedule` MUST have an explicit `ONE_WAY` or `LOOP` type.
- Ordered stops are `RailTrainStop` values containing a `RailStationId` plus generic stop settings for future expansion.
- A timetable stop MUST NOT reference a raw track graph node.
- A station restricts stopping direction: a train MUST stop only while facing the station's stop direction. Other trains
  MAY pass through in reverse but MUST NOT reverse-stop at the station.
- A station itself MUST NOT load/unload cargo and MUST NOT provide fuel or other complex station features.
- A station MAY have a behind-chain of freight platforms or other stations.
- When a train stops at a station, all freight platforms in that station's behind-chain MUST participate.
- Each freight platform in the main mod MUST have a binary `LOAD` or `UNLOAD` operation. Loading or unloading installs or
  removes a whole cargo body, excluding the underframe.
- The main departure condition MUST be `OPERATION_COMPLETE`: the train departs after all participating freight platforms
  complete their operations.
- If a station has no freight platforms behind it, the train MUST stop and then depart.
- If a freight platform cannot complete its operation, the train MUST wait until the operation can complete.
- `StationOperation` and `DepartureCondition` MUST be extension points for addons. The main mod registers only
  `LOAD`/`UNLOAD` and `OPERATION_COMPLETE`.
- Schedules MUST be started and stopped by the player, and schedule state MUST be persisted.
- Empty or single-stop schedules MUST be rejected at start.
- Schedules MUST be editable while running; rerouting MUST project the current position to the nearest reachable node and
  pathfind to the next stop.
- A `LOOP` schedule MUST continue from the first stop after the last stop. A `ONE_WAY` schedule MUST terminate after the
  last stop.
- Automatic movement is bidirectional on the track graph; the pathfinder may route to the next stop in either direction.

## Signals and Dispatch

- Player-visible signal state MUST be limited to `RED` and `GREEN`. Complex aspects, additional signal types, and custom
  signal states are addon extension points.
- A block signal is directional: it protects the next section in its facing direction.
- A path signal is directional: at a junction entrance, it reserves the full conflict path through the junction to the
  chosen exit, and the reservation is released after the train exits.
- Automatic routes MUST lock at departure and reserve/release sections stepwise.
- When a red signal or reserved section blocks the route, an automatic train MUST stop and wait.
- When the graph changes, an automatic train MUST try to reroute. If no route is available, or the new route is blocked,
  the train MUST stop and wait.
- Manual trains may ignore signals. They still participate in collision detection and section locking.

## Collision and Derailment

- A collision involving trains MUST derail all involved trains and stop them.
- Derailment MUST pause the automatic schedule and release current route/reservations.
- The current section occupied by a derailed train MUST remain locked until the train is reset and leaves that section.
- Other automatic trains MUST reroute around a locked section when possible; otherwise they MUST stop and wait.
- A player MUST use a tool to right/reset a derailed train.
- Train damage and repair are not planned gameplay; the tool only returns the train to a movable state.

## API Shape

These names are part of the contract but are not implemented in this documentation-only change:

- `api.rail.RailNetworkService`
- `api.rail.RailNetworkSnapshot`
- `api.rail.graph.TrackCellData`
- `api.rail.dispatch.RailTrainSchedule`
- `api.rail.dispatch.RailTrainStop`
- `api.rail.dispatch.RailControlMode`
- `api.rail.station.RailStationId`
- `api.rail.station.StationOperation`
- `api.rail.station.DepartureCondition`

Existing `api.rail.graph.RailGraphView` and `api.rail.dispatch.DispatchService` remain foundations.

`RailTrainStop` contains a `RailStationId` plus generic stop settings. It does not contain a raw graph node ID.

API records are limited to IDs, requests, and result snapshots. Domain records such as graph nodes, edges, sections,
train aggregates, and persistence structures live in `core.rail` or `internal.rail`.

## Acceptance Scenarios

1. Two automatic trains MUST NOT occupy the same section; the second train MUST stop before a red block signal.
2. A path signal MUST reserve the full junction path until the train exits the chosen exit.
3. A manual train ignoring a signal and colliding with an automatic train MUST derail both; the occupied section MUST
   stay locked until reset and leave.
4. Removing track ahead MUST trigger reroute when possible; if no route exists, the automatic train MUST stop and wait.
5. Saving and reloading MUST restore graph cache, timetable, and train state; stale cache MUST be validated locally and
   rebuilt as a connected component.
6. A `ONE_WAY` schedule MUST stop after the last stop; a `LOOP` schedule MUST continue from the first stop.
7. Editing a schedule while running MUST project the train to the nearest node and reroute to the next stop.
8. KubeJS and other integrations MUST only read immutable `RailNetworkSnapshot`; they MUST NOT mutate graph, sections,
   dispatch, or train state.
9. Block signals MUST show red when the protected section is occupied and green when it is free.
10. Path signals MUST reserve the full junction path and release it after the train exits.
11. A train MUST be allowed to pass through a station in reverse, but MUST NOT reverse-stop at that station.
12. All freight platforms in a station's behind-chain MUST execute their configured operations; the train departs after
    all operations complete.
13. A station without freight platforms MUST stop the train and then depart.
14. The main mod MUST only provide `LOAD` and `UNLOAD` station operations and the `OPERATION_COMPLETE` departure
    condition.
15. If an operation cannot complete, the train MUST wait; after completion, the train continues.
16. A simple straight or diagonal 45 track cell MUST NOT use a block entity.
17. A crossing, curve, curve ramp, or signal cell MUST use a block entity.
18. The graph source MUST read the block entity first and fall back to BlockState direction.
19. Upgrading or downgrading a track cell MUST preserve existing placement data.
20. Simple cells MUST use datagen models; complex cells MUST use the block entity renderer.
21. Track collision MUST exist only inside the owning block's 16x16 bounds.
22. Complex collision MUST be the union of the cell's rail collision shapes.
23. A track cell MUST occupy only its own block slot and MUST NOT be waterlogged.
24. Visual overflow MUST NOT create neighboring occupancy or collision.
25. Visual overlap MUST NOT create track graph connections.
26. Train entities MUST ignore track collision but MUST collide with ordinary blocks and other entities.
27. Reloading a save MUST restore complex block entity data and collision shapes.

## Related Documents

- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
- `docs/decisions/0003-rail-runtime-threading-and-snapshots.md`
- `docs/decisions/0004-rail-signals-dispatch-and-train-aggregate.md`
