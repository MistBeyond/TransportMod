# AGENTS.md

## Core Requirement

- **Must use IDEA MCP for code operations** (e.g., rename, refactor, search, debug, format).  
  If IDEA MCP is not available or not started, **immediately stop and inform the user** that IDEA MCP support is
  required.

## Prohibited Actions

1. **Do not modify any files outside the project source code and build scripts** (e.g., system files, Gradle wrapper
   files, library caches).
2. **Do not add any dependencies without explicit user consent**. If needed, present options and await user decision.
3. **Do not change any project dependency definitions** (including but not limited to `build.gradle`,
   `build.gradle.kts`, `settings.gradle`, `settings.gradle.kts`, `gradle.properties`) without user approval. This
   includes NeoForge version, mod versions (e.g., Jade), etc.
4. **Do not download anything without user consent**. Show exact actions (e.g., using `gradlew` for dependency
   management) and await approval.
5. **Do not hand-write generated resource/model files. Generate models, blockstates, language files, tags, recipes,
   and loot tables through datagen or reuse existing vanilla resources. Hand-writing model files is forbidden by
   default; custom model templates are the only accepted exception.**
6. **Do not embed local absolute paths in project files** (e.g., paths to Gradle caches, Maven repositories, or IDE
   settings).

## Docs Map

**README-first navigation**: every docs directory is navigated through its README — the README of a directory is its
index and the authority on what belongs in it. Before reading or modifying anything under `docs/`, read the README of
the directory you are entering (start at `docs/README.md`; subdirectories have their own READMEs, e.g.
`docs/roadmap/README.md`), and follow it to decide which documents to read. The README wins when a document conflicts
with it.

Quick reference to the top-level tree (full index and content ownership: `docs/README.md`):

1. `docs/README.md` - Tree index and content ownership. Read first; the entry point for all documentation navigation.
2. `docs/roadmap.md` - Implementation order (not gameplay order). Read at the start of each implementation task.
3. `docs/architecture.md` - Package placement, dependencies, layers; user rulings live in `docs/decisions/`. Read
   before structural changes; if a placement is undocumented, ask the user and record it in `docs/decisions/`.
4. `docs/design-principles.md` - Rationale behind the `Design Principles` section. Read when a design decision is
   ambiguous or an exception seems justified.

Use them together: `roadmap.md` decides what to build, `architecture.md` where code belongs, `design-principles.md`
how to design. Read the ones that apply, then let each directory's README guide further navigation.

## Workflow

1. **Before modification**: Navigate the docs via READMEs (README-first, per `## Docs Map`), read the applicable
   docs, then read relevant code and documentation, understand existing features and interfaces, and make a plan.
2. **Ensure package nullability**: Every Java package must include a `package-info.java` annotated with `@NullMarked`
   (JSpecify). Use `$ensure-package-info` to create any missing files.
3. **After modification**:
    - Use IDEA MCP to analyze the project, check for errors, and fix them. Fix warnings where possible; ignore only if
      unavoidable (e.g., fixed Guava version requiring beta graph API).
    - Use IDEA MCP to format code.
    - Do **not** commit code; commits are only performed upon explicit user request.

## Design Principles

1. **Contracts live in `api`; `core` carries gameplay**: `api.<feature>` is pure contracts; `core.<feature>` owns
   gameplay, rules, and `api` implementations; `internal.<feature>` is a minimal implementation-detail placeholder.
2. **Dependencies stay acyclic**: `api` depends on no project packages; `core` depends on `api`/`config`/`util`;
   content packages are final presentation and must not be depended on by `core`.
3. **Prefer composition to inheritance**: Subclass framework types only where NeoForge/Minecraft requires it.
4. **Prefer immutable data and explicit JSpecify nullability**: Avoid null literals and shared mutable state in new
   code; every package is `@NullMarked`.
5. **Cross-feature access goes through public APIs**: Prefer `api`; content packages use `core`; direct `internal`
   access only when concrete code needs it.
6. **External integrations live in `integration`**: JEI, Jade, KubeJS, and other addon integration goes under
   `integration`; no top-level addon packages. KubeJS defaults to `api`.
7. **Do not use NeoForge `@OnlyIn`**: Keep client-only code in `client` packages and wire it through client entry
   points.

Detailed rationale, examples, and exception criteria: `docs/design-principles.md`.

## Knowledge Strategy

- **Priority order**:
    1. Javadoc (inline documentation)
    2. Code itself
    3. Official documentation
    4. Web search
    5. Model's internal knowledge
- **Must combine with documentation and code; never answer solely from internal knowledge.**
- **Read project and library sources through IDEA MCP**: Prefer IDEA MCP for Javadoc and source navigation over manually
  extracting JAR archives. If Javadoc is insufficient or guidance is needed, perform web search. For specific targets,
  select the correct version from the official website and read documentation for that version. **If documentation is
  inaccessible, do not silently skip; inform the user**.
- **Mandatory official documentation sites** (choose the version matching your project dependency):
    - NeoForge: https://docs.neoforged.net/docs/gettingstarted/
    - Mixin: https://github.com/SpongePowered/Mixin/wiki

## Tool Usage (including but not limited to)

- All code operations (formatting, search, refactoring, debugging) must be executed through IDEA MCP.
- Prefer IDEA MCP's tool list for IDEA-supported operations; avoid using IDEA MCP's built-in terminal emulator.
- Run shell commands in the agent's own built-in terminal.
- Gradle tasks (e.g., build, test, datagen) should run through IDEA MCP first; fall back to the `gradlew` CLI only when
  IDEA MCP cannot run them.
