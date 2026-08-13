# Trains

Status: `contract` (train aggregate and control mode rules; cargo composition still planned)

Parent overview: `docs/roadmap/rail.md`

## Summary

Trains consist of a locomotive and a freight portion. The freight portion uses a freight underframe plus a replaceable cargo body.

The runtime train model is binding in `docs/roadmap/rail/runtime-contract.md`. Manual and automatic trains share one
server-side aggregate; they differ only in control source.

## Confirmed Directions

- All trains use `core.rail.RailTrainAggregate`.
- Manual and automatic are `RailControlMode` values, not separate train classes.
- Manual mode is driven by player input; automatic mode is driven by `RailTrainSchedule`.
- Switching to manual pauses the automatic schedule and releases its route/reservations.
- Automatic schedules use explicit `ONE_WAY` or `LOOP` types and ordered generic stops.
- A `LOOP` schedule continues from the first stop after the last stop; a `ONE_WAY` schedule terminates after the last stop.
- Schedules are persisted, editable while running, and reroute from the nearest reachable node.
- Collisions derail all involved trains. The current section stays locked until the train is reset and leaves.
- A player tool resets derailed trains; damage and repair are not planned gameplay.

## Scope

- Train composition and connection order.
- Locomotive driving and controls.
- Connection rules between freight underframes and replaceable cargo bodies.
- Replaceable cargo body types, including item or fluid storage.
- Players sitting in the locomotive and standing on train parts while moving.
- The relationship between separate entity presentation and the server aggregate model.

## Open Questions

- Exact entity sync and multiplayer presentation rules in the hybrid model.
- In which states underframes and cargo bodies can be swapped.
- How much customization replaceable cargo bodies support.
