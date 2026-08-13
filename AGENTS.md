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

## Workflow

1. **Before modification**: Read `docs/roadmap.md` to determine implementation order (roadmap defines code
   implementation order, not gameplay phase order). Then read relevant code and documentation, understand existing
   features and interfaces, make a plan.
2. **Respect architecture decisions**: For structural changes (new packages, moving or extracting classes, changing
   dependency direction), consult `docs/architecture.md` first. If a placement is not documented there, ask the user for
   the intended package/design and record the decision after confirmation.
3. **Ensure package nullability**: Every Java package must include a `package-info.java` annotated with `@NullMarked`
   (JSpecify). Use `$ensure-package-info` to create any missing files.
4. **After modification**:
    - Use IDEA MCP to analyze the project, check for errors, and fix them. Fix warnings where possible; ignore only if
      unavoidable (e.g., fixed Guava version requiring beta graph API).
    - Use IDEA MCP to format code.
    - Do **not** commit code; commits are only performed upon explicit user request.

## Design Principles

1. **Contracts live in `api`; `core` carries gameplay**: `api.<feature>` is pure; `core.<feature>` contains gameplay,
   business rules, services, state machines, public entry points, and `api` implementations. `internal.<feature>` is a
   minimal implementation-detail placeholder whose boundary is defined by code practice.
2. **Dependencies stay acyclic**: `api` depends on no project packages. `core` depends on `api`/`config`/`util`.
   Content packages are final presentation and must not be depended on by `core`. `internal` dependency rules are left
   open until code practice defines them.
3. **Prefer composition to inheritance**: Extend framework classes only when NeoForge/Minecraft requires it.
4. **Prefer immutable data and explicit JSpecify nullability**: Avoid null literals and shared mutable state in new
   code.
5. **Cross-feature access goes through public APIs**: Prefer `api`; content packages use `core`; direct `internal`
   access is allowed when concrete code needs it.
6. **External integrations live in `integration`**: JEI, Jade, KubeJS, and any other addon or third-party mod
   integration belongs under `integration`; do not create top-level addon packages outside it. KubeJS defaults to `api`,
   may use `core`, and logs rare `internal` exceptions.

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
