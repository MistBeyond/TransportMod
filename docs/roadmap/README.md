# Roadmap Docs Overview and Content Ownership

This document is the **index of the `roadmap/` directory**: the overview of what each direct entry contains and the
authority for what content belongs in each of them. Read it before adding or moving documentation. In short, this README
is the **metadata of this documentation subtree**.

## Purpose

`roadmap/` holds the per-mode detail documents behind `docs/roadmap.md`: each transport mode (rail, truck, drone, water)
and the KubeJS integration track gets its own document with gameplay and implementation detail, and the rail mode gets a
subdirectory of binding contracts and implementation notes. The implementation order itself lives in
`docs/roadmap.md`; this subtree only holds the detail that each node references.

## Overview

| Path        | Purpose                                                               |
|-------------|-----------------------------------------------------------------------|
| `README.md` | This document: directory overview and content ownership rules         |
| `rail.md`   | Railway mode overview and index; roadmap node 1                       |
| `rail/`     | Railway contracts and implementation notes; index in `rail/README.md` |
| `truck.md`  | Truck mode detail; roadmap node 2 (placeholder)                       |
| `drone.md`  | Drone mode scope draft; roadmap node 3 (placeholder)                  |
| `water.md`  | Water mode scope draft; roadmap node 4 (placeholder)                  |
| `kubejs.md` | KubeJS integration capability scope; parallel track from node 0       |

## Content Ownership

| Document    | Belongs here                                                                   | Does not belong here                                                                   |
|-------------|--------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| `rail.md`   | Railway overview, design summary, links to the `rail/` contracts               | Contract detail (→ `rail/`), implementation order (→ `docs/roadmap.md`)                |
| `truck.md`  | Truck gameplay and implementation detail, KubeJS access points                 | Architecture rulings (→ `docs/decisions/`), implementation order (→ `docs/roadmap.md`) |
| `drone.md`  | Drone scope draft and open questions                                           | Same as `truck.md`                                                                     |
| `water.md`  | Water scope draft and open questions                                           | Same as `truck.md`                                                                     |
| `kubejs.md` | KubeJS capability scope across all modes                                       | Mode-specific content registration design (→ the mode docs)                            |
| `rail/`     | Railway binding contracts and implementation notes (owned by `rail/README.md`) | Global rules, other modes' detail                                                      |

## Ownership Rules

1. This directory mirrors the nodes in `docs/roadmap.md`; node status and implementation order live there — do not
   duplicate them here.
2. Each mode document carries that mode's gameplay and implementation detail only; binding contracts for a mode live in
   that mode's own subdirectory (e.g. `rail/`), and accepted user rulings live in `docs/decisions/`.
3. Binding contracts win over concept notes on conflict; see the document hierarchy in the document-editing-rules skill.
4. `rail/` content is owned by `rail/README.md`; this README indexes direct entries only.
5. Terminology stays consistent with the rest of the docs (e.g. `World Grid`, simple/complex cells, baked-model route,
   `TrackPlacement`).
