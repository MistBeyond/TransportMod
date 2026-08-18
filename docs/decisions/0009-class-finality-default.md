# ADR 0009: Class Finality Default — Non-Final Unless a Concrete Reason

Status: Accepted

Date: 2026-08-17

## Context

The codebase's working practice defaulted implementation classes to `final` (26 `final` classes exist; only
framework-required types — `Block`, `BlockEntity`, `Item`, `Entity`, renderers, and the `@Mod` entry — are non-final).
No document mandated this: the "final" occurrences in the docs refer to end-consumer package layering, final fields,
and a warning against finalizing the `internal` layer, never to Java class finality. The habit derived from Design
Principles 3 and 4. Other mods may legitimately want to extend our classes (for example, subclassing a service or a
utility), and a final-by-default habit blocks that for no recorded reason.

## Decision

- Project classes default to **non-final**. Mark a class `final` only for a concrete reason — for example an invariant
  that subclassing would break, or when the class is a record.
- Data carriers stay records; Java records are implicitly final, and this rule does not change them.
- Framework-required subclassing (blocks, block entities, items, entities, renderers, the `@Mod` entry) stays exactly
  as governed by Design Principle 3.
- The rule applies to new code and future refactors. Existing `final` classes are not required to be reopened; code
  owners may open them when a real extension need appears.

## Consequences

- Other mods can extend our classes by subclassing where that is sound. Interfaces remain the preferred extension point
  (Design Principle 5); non-final simply stops blocking subclassing as a fallback.
- Design Principle 3 (composition over inheritance) still applies: non-final opens the door, it does not encourage
  subclassing.
- `final` stays legitimate where subclassing would break correctness, and records are unaffected.

## Related Documents

- `docs/design-principles.md` (Principle 3)
- `docs/decisions/0001-api-core-internal.md` (package layers — unaffected)