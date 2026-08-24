# ADR 0010: Rail Signal Runtime Defaults — Tail Exit, Contention, ERROR Visual, and Re-facing

Status: Accepted

Date: 2026-08-20

## Context

ADR 0007 and ADR 0008 fixed path-signal evaluation, placement, conflict definition, and the ERROR state, but
deliberately left a small set of runtime defaults open: the precise computation of the train's "tail" for release
timing, how contested atomic grants are arbitrated, the physical form of the `ERROR` indicator, and how a signal's
facing is chosen and changed. This decision pins those defaults so implementation can start without further questions.

## Decision

- Tail exit (release timing): a train's tail has exited the junction when the set of cells its body currently occupies
  no longer intersects the conflict path. Once a junction route reservation is granted, the related entrance signals
  stay `RED` until that point; the full conflict-path reservation is then released as a whole at once (per-segment
  progressive release remains a future optimization).
- Contention arbitration: when multiple automatic trains compete for the same conflict path at the same time, the
  reservation grant follows first-come-first-served order by request arrival. The losing trains stop, wait, and retry
  per the runtime-contract wake-up rules (event-driven retry with optional low-frequency fallback). This covers
  signal-grant contention only; a general dispatch priority/scheduling model remains open.
- ERROR visual: the `ERROR` indicator is rendered as an exclamation overlay on the signal block's side and is also
  marked in the F3 debug overlay. `ERROR` remains a configuration/health indicator, not a third aspect
  (`SignalAspect` stays `{RED, GREEN}`).
- Placement interaction: a signal's facing direction at placement is the player's look direction. Re-facing a signal is
  done only by breaking and replacing it; there is no in-place rotation interaction.

## Consequences

- `docs/roadmap/rail/runtime-contract.md` refines the tail clause with the zero-intersection definition and adds the
  first-come-first-served arbitration MUST; acceptance scenarios 31 and 32 are added.
- `docs/roadmap/rail/sections.md` records the same defaults in its confirmed directions.
- `docs/roadmap/rail/dispatch.md` narrows its open question to general dispatch priority/schedules (beyond the
  first-come-first-served signal-grant arbitration).
- Implementation can now start the signal semantic layer without choosing these defaults itself. The code-side deltas
  remain those listed in ADR 0007/0008: a `PATH` branch in signal-state resolution (the current `SignalStateResolver`
  treats `PATH` like `BLOCK`), an atomic junction reservation grant with whole-at-once release, two signal items with
  distinct models, a same-cell/same-facing placement constraint, F3 marker specialization plus error markers, and now
  also the in-world exclamation overlay and break-and-replace re-facing. `SignalState`'s existing `error` boolean
  carries the health state; `SignalAspect` stays `{RED, GREEN}`.
- Deferred as future work by prior decisions remains unchanged: reserved-path highlighting (C2) and the player-facing
  "red but expected to clear" vs "deadlock" distinction (D4).

## Related Documents

- `docs/roadmap/rail/runtime-contract.md`
- `docs/roadmap/rail/sections.md`
- `docs/roadmap/rail/dispatch.md`
- `docs/decisions/0007-rail-path-signals-and-blocked-train-wakeup.md`
- `docs/decisions/0008-rail-signal-placement-conflict-and-error-state.md`
