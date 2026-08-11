# Architecture Map

This document is the authoritative source for concrete package placement and the user's architecture rulings. Read it
before any structural change: new packages, moving or extracting classes, or changing dependency direction. General
design principles live in `design-principles.md`; this map records where things actually belong.

## How to use

- Before a structural change, check this map first.
- If the placement is documented, follow it without redesigning.
- If it is not documented, ask the user with concrete options (for example: "put this helper in `util` (recommended)
  or `core`?"). After the user confirms, add a row to "Decision log".
- A recorded user decision overrides generic best practice. Do not invent package boundaries.

## Package map

| Package            | Responsibility                                                                                 | Belongs here                                                                 | Does not belong here                                                      |
|--------------------|------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------|---------------------------------------------------------------------------|


## Decision log

| Date       | Decision                                                                                                                                                                                                                                                                                                 | Reason                                                                                                                      |
|------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|


## Feature specs

`docs/features/` holds user-authored, code-specific specs. A spec is active only when the user explicitly created or
maintains it; active specs declare their own scope and are referenced from the code they cover (e.g., code Javadoc).
Read an active spec before modifying the code it covers. Do not create, update, or delete feature specs without the
user's approval.
