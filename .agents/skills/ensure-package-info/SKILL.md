---
name: ensure-package-info
description: Create or verify JSpecify @NullMarked package-info.java files for every Java package. Use when a Java project requires each package to declare nullability, when new packages lack package-info.java, or when enforcing package-info.java coverage.
---

# Ensure Package Info

## Runner Requirement

**Must run the script with `python` or `uv`:**

- `python scripts/ensure_package_info.py`
- `uv run scripts/ensure_package_info.py`

Do not execute the script with any other interpreter.

## Workflow

1. Run `scripts/ensure_package_info.py` from the project root. It scans `src/*/java` source roots by default.
2. Treat every directory that contains at least one `.java` file as a Java package.
3. If `package-info.java` is missing, create it with JSpecify's `@NullMarked` and the correct package declaration.
4. If `package-info.java` already exists, leave it unchanged.
5. Preview changes with `--dry-run`. Scan other locations by passing roots explicitly, for example
   `python scripts/ensure_package_info.py path/to/java-src`.
6. Report the created and already-present counts to the user. Do not hand-write these files; always run the script.
