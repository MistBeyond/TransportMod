# Stations

Status: `contract` (station and freight platform design; block implementation deferred)

Parent overview: `docs/roadmap/rail.md`

## Summary

Stations are timetable stops. A station itself only defines where a train may stop; freight platforms attached behind the
station perform loading and unloading.

Station and freight platform blocks are required later for the timetable MVP, but this documentation-only change does not
implement them.

## Confirmed Directions

- Timetable stops reference `RailStationId`; they do not reference raw track graph nodes.
- A station restricts stopping direction: a train may stop only while facing the station's stop direction. Other trains
  may pass through in reverse but may not reverse-stop.
- A station itself does not load/unload cargo and does not provide fuel or other complex station features.
- A station may have a behind-chain of freight platforms or other stations.
- When a train stops at a station, all freight platforms in that station's behind-chain participate in the stop.
- Each freight platform in the main mod has a binary operation: `LOAD` or `UNLOAD`.
- Loading or unloading installs or removes a whole cargo body, excluding the freight underframe.
- The main departure condition is `OPERATION_COMPLETE`: the train departs after all participating platforms complete.
- A station without freight platforms stops the train and then departs.
- If a freight platform cannot complete its operation, the train waits.
- `StationOperation` and `DepartureCondition` are extension points for addons; the main mod provides only
  `LOAD`/`UNLOAD` and `OPERATION_COMPLETE`.

## Scope

- Station stop direction.
- Station behind-chain construction.
- Freight platform load/unload configuration.
- Timetable stop references.
- Departure conditions and addon extension points.
- Cargo body install/remove behavior.

## Open Questions

- Exact station and freight platform block models, placement, and visual form.
- How players edit station behind-chains and platform operations in-game.
- Which addon station extensions, such as fuel, will be exposed by future APIs.
