# Dispatch

Status: `planned` (placeholder; content pending)

Parent overview: `docs/roadmap/rail.md`

## Summary

The dispatch system manages rail sections, signal control, and autonomous train operation while keeping automatic and
manual driving compatible.

## Scope

- Rail section definition and ownership.
- Block signals and path signals.
- Autonomous train dispatch and route selection.
- Conflict, deadlock, and congestion handling.
- Transition rules between manual driving and autonomous dispatch.

## Open Questions

- The state semantics of block signals and path signals.
- Path reservation and release rules.
- How dispatch priority and schedules are defined.
- How dispatch logic stays independent of concrete Minecraft blocks.
