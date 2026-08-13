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

- Station blocks, cargo loading/unloading, freight bodies, and station forms.
- Truck, drone, water transport, and KubeJS content registration.

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

## Train Aggregate and Control Modes

- All trains use the same `core.rail.RailTrainAggregate` model.
- Manual and automatic are `RailControlMode` values, not separate train classes.
- Manual mode is driven by player input; automatic mode is driven by `RailTrainSchedule`.
- Switching to manual MUST pause the automatic schedule and release its route/reservations.
- The entity is presentation only; it MUST NOT own route planning, graph state, or dispatch decisions.
- Derailment state MUST belong to the train aggregate.

## Timetable

- `RailTrainSchedule` MUST have an explicit `ONE_WAY` or `LOOP` type.
- Ordered stops are `RailTrainStop` values containing a track node ID plus generic settings for future expansion.
- The MVP default arrival behavior is: stop at the node, then depart toward the next stop. Per-stop settings may override
  this later.
- Schedules MUST be started and stopped by the player, and schedule state MUST be persisted.
- Empty or single-stop schedules MUST be rejected at start.
- Schedules MUST be editable while running; rerouting MUST project the current position to the nearest reachable node and
  pathfind to the next stop.
- A `LOOP` schedule MUST continue from the first stop after the last stop. A `ONE_WAY` schedule MUST terminate after the
  last stop.
- Automatic movement is bidirectional on the track graph; the pathfinder may route to the next stop in either direction.

## Signals and Dispatch

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
- `api.rail.dispatch.RailTrainSchedule`
- `api.rail.dispatch.RailTrainStop`
- `api.rail.dispatch.RailControlMode`

Existing `api.rail.graph.RailGraphView` and `api.rail.dispatch.DispatchService` remain foundations.

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

## Related Documents

- `docs/decisions/0002-rail-graph-ownership-and-lifecycle.md`
- `docs/decisions/0003-rail-runtime-threading-and-snapshots.md`
- `docs/decisions/0004-rail-signals-dispatch-and-train-aggregate.md`
