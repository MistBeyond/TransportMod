#!/usr/bin/env python3
"""Create a README.md for a new subdirectory under the project's docs/ directory.

Must be run with `uv` first, or `python`:
    uv run scripts/new_docs_subdir.py <subdir>
    python scripts/new_docs_subdir.py <subdir>

The script fills a skeleton README from the skill template and writes it into
docs/<subdir>/README.md. It validates the target path strictly so that files are
never created outside docs/ or over an existing README.

The docs root is anchored to the project root derived from this script's own
location (this skill lives at .agents/skills/document-editing-rules/scripts/),
so the script does NOT depend on the current working directory.
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

# Project root: this script lives at .agents/skills/document-editing-rules/scripts/,
# so parents[4] is the repository root regardless of the working directory.
PROJECT_ROOT = Path(__file__).resolve().parents[4]
# Project docs root, anchored to the project (never relative to the cwd).
DOCS_ROOT = PROJECT_ROOT / "docs"
# Template location relative to this file's skill directory.
TEMPLATE_PATH = (
    Path(__file__).resolve().parent.parent / "templates" / "docs-subdir-README.template.md"
)

# Windows forbids these characters in path components, plus we reject path traversal,
# drive letters, and home expansion on any platform.
FORBIDDEN_CHARS = re.compile(r'[<>"|?*\x00-\x1f]')


def check_project_layout() -> None:
    """Fail fast when the anchored docs root or its index is missing.

    Guards against this skill having been moved or copied into a different
    project: never silently write into a wrong or nonexistent docs/ location.
    """
    if not DOCS_ROOT.is_dir():
        print(
            f"error: docs root not found at expected location: {DOCS_ROOT}",
            file=sys.stderr,
        )
        sys.exit(2)
    if not (DOCS_ROOT / "README.md").is_file():
        print(
            f"error: docs index not found at {DOCS_ROOT / 'README.md'}; "
            "is this the correct project?",
            file=sys.stderr,
        )
        sys.exit(2)


def validate_subdir(subdir: str) -> Path:
    """Validate the subdir argument and return the resolved target directory.

    Exits with a non-zero code (and never writes anything) when the path is not a
    safe, simple relative path inside docs/.
    """
    if not subdir:
        print("error: subdir must not be empty", file=sys.stderr)
        sys.exit(2)

    # Reject absolute paths and root-relative paths (leading slash or backslash).
    if subdir.startswith("/") or subdir.startswith("\\"):
        print(f"error: subdir must be a relative path, got: {subdir}", file=sys.stderr)
        sys.exit(2)

    # Reject drive letters (e.g. C:), home expansion, and Windows-forbidden characters.
    if ":" in subdir:
        print(f"error: subdir must not contain ':' (drive letter), got: {subdir}", file=sys.stderr)
        sys.exit(2)
    if "~" in subdir:
        print(f"error: subdir must not contain '~', got: {subdir}", file=sys.stderr)
        sys.exit(2)
    if FORBIDDEN_CHARS.search(subdir):
        print(f"error: subdir contains forbidden characters: {subdir}", file=sys.stderr)
        sys.exit(2)

    # Reject traversal: split on both separators and check every component.
    parts = re.split(r"[\\/]+", subdir)
    for part in parts:
        if part in ("", ".", ".."):
            print(f"error: subdir must not contain empty, '.' or '..' components, got: {subdir}", file=sys.stderr)
            sys.exit(2)

    # Reject a subdir that repeats the docs root name (e.g. "docs" or "docs/rail").
    if parts[0] == DOCS_ROOT.name:
        print(
            f"error: subdir must not start with the docs root name '{DOCS_ROOT.name}': {subdir}",
            file=sys.stderr,
        )
        sys.exit(2)

    target_dir = (DOCS_ROOT / subdir).resolve()
    docs_root_resolved = DOCS_ROOT.resolve()

    # Defensive second check: the resolved target must stay inside docs/.
    if not target_dir.is_relative_to(docs_root_resolved):
        print(f"error: target escapes docs/ directory: {target_dir}", file=sys.stderr)
        sys.exit(2)
    # The subdirectory itself must not be the docs root.
    if target_dir == docs_root_resolved:
        print("error: subdir must not be the docs/ root itself", file=sys.stderr)
        sys.exit(2)

    return target_dir


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Create a README.md for a new subdirectory under docs/."
    )
    parser.add_argument("subdir", help="Relative path under docs/, e.g. roadmap/rail")
    parser.add_argument("--title", help="README title; defaults to the last path component")
    parser.add_argument(
        "--dry-run", action="store_true", help="Print what would be created without writing anything"
    )
    args = parser.parse_args()

    check_project_layout()
    target_dir = validate_subdir(args.subdir)
    readme_path = target_dir / "README.md"

    if readme_path.exists():
        print(f"error: README already exists, refusing to overwrite: {readme_path}", file=sys.stderr)
        return 2

    if not TEMPLATE_PATH.is_file():
        print(f"error: template not found: {TEMPLATE_PATH}", file=sys.stderr)
        return 2

    title = args.title or target_dir.name

    template = TEMPLATE_PATH.read_text(encoding="utf-8")
    content = template.replace("{TITLE}", title).replace("{DIR}", target_dir.name)

    if args.dry_run:
        print(f"dry-run: would create directory: {target_dir}")
        print(f"dry-run: would write README: {readme_path}")
        print(f"dry-run: title: {title}")
        return 0

    target_dir.mkdir(parents=True, exist_ok=True)
    with readme_path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(content)

    print(f"created directory: {target_dir}")
    print(f"created README: {readme_path}")
    print("next: fill in every _TODO_ section (Purpose, Overview, Content Ownership, Ownership Rules).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
