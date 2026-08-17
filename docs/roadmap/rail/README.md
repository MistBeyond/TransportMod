# Rail Docs Overview and Content Ownership

This document is the **index of the `rail/` directory**: the overview of what each direct entry contains and the
authority for what content belongs in each of them. Read it before adding or moving documentation. In short, this README
is the **metadata of this documentation subtree**.

## Purpose

`rail/` holds the railway contracts and implementation notes for roadmap node 1: the binding runtime contract for the
Graph + Dispatch MVP, the confirmed-direction documents for tracks, sections, dispatch, trains, and stations, and the
implementation notes behind them. The mode overview and design summary live one level up in `rail.md`.

## Overview

| Path                  | Purpose                                                                 |
|-----------------------|-------------------------------------------------------------------------|
| `README.md`           | This document: directory overview and content ownership rules           |
| `runtime-contract.md` | Binding Graph + Dispatch MVP runtime contract; wins on conflict         |
| `tracks.md`           | Track rules: placement, track types, `World Grid`, graph integration    |
| `sections.md`         | Section and signal rules; signal state semantics                        |
| `dispatch.md`         | Dispatch rules: timetables, route planning, reservations, conflicts     |
| `trains.md`           | Train aggregate and control modes (manual/automatic), cargo composition |
| `stations.md`         | Station and freight platform design; block implementation deferred      |
| `implementation.md`   | Implementation notes: package placement, rendering, collision shapes    |

## Content Ownership

| Document              | Belongs here                                                                 | Does not belong here                                                                       |
|-----------------------|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| `runtime-contract.md` | Binding behavior: ownership, lifecycle, threading, persistence, collision    | Gameplay intent, unconfirmed detail                                                        |
| `tracks.md`           | Confirmed track directions, placement rules, track types                     | Binding runtime behavior (→ `runtime-contract.md`), implementation (→ `implementation.md`) |
| `sections.md`         | Confirmed section/signal directions, signal state semantics                  | Same as `tracks.md`                                                                        |
| `dispatch.md`         | Confirmed dispatch directions, timetable and reservation rules               | Same as `tracks.md`                                                                        |
| `trains.md`           | Confirmed train aggregate/control-mode directions, cargo composition         | Same as `tracks.md`                                                                        |
| `stations.md`         | Confirmed station/freight-platform design                                    | Same as `tracks.md`                                                                        |
| `implementation.md`   | Implementation notes: package placement, baked-model route, collision strips | Gameplay behavior, accepted rulings (→ `docs/decisions/`)                                  |

## Ownership Rules

1. `runtime-contract.md` is the binding contract for the Graph + Dispatch MVP; where it conflicts with concept notes,
   this document wins.
2. Topic documents record confirmed directions per topic; binding runtime behavior belongs in `runtime-contract.md`
   and is not duplicated here.
3. Accepted decisions live in `docs/decisions/` as ADRs; add a new ADR (and index it) before changing an accepted
   ruling.
4. Status headers in this directory must stay in sync with the mode overview `docs/roadmap/rail.md`.
5. This README indexes direct entries only; nested subdirectories are owned by their own READMEs.
