#!/usr/bin/env python3
"""Create missing JSpecify @NullMarked package-info.java files for Java packages.

Must be run with `python` or `uv`:
    python scripts/ensure_package_info.py
    uv run scripts/ensure_package_info.py
"""

from __future__ import annotations

import argparse
import keyword
import os
import sys
from pathlib import Path

SKIP_DIRS = {".git", ".idea", ".gradle", "build", "target", "out", "node_modules"}

TEMPLATE = """@NullMarked
package {package};

import org.jspecify.annotations.NullMarked;
"""


def discover_roots() -> list[Path]:
    src = Path("src")
    roots = sorted(p for p in src.glob("*/java") if p.is_dir()) if src.is_dir() else []
    if not roots:
        roots = [p for p in (Path("src/main/java"), Path("src/test/java")) if p.is_dir()]
    return roots


def is_valid_package_name(package: str) -> bool:
    return all(part.isidentifier() and not keyword.iskeyword(part) for part in package.split("."))


def find_package_dirs(root: Path) -> list[Path]:
    package_dirs: list[Path] = []
    for current, child_dirs, files in os.walk(root):
        child_dirs[:] = sorted(
            d for d in child_dirs if d not in SKIP_DIRS and not d.startswith(".")
        )
        if any(name.endswith(".java") for name in files):
            package_dirs.append(Path(current))
    return package_dirs


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Create missing @NullMarked package-info.java files for Java packages."
    )
    parser.add_argument("roots", nargs="*", help="Java source roots; defaults to src/*/java")
    parser.add_argument("--dry-run", action="store_true", help="Print actions without writing files")
    args = parser.parse_args()

    roots = [Path(root) for root in args.roots] or discover_roots()
    if not roots:
        print("error: no Java source roots found; pass roots explicitly", file=sys.stderr)
        return 2

    created = already_present = 0
    for root in roots:
        if not root.is_dir():
            print(f"error: root does not exist: {root}", file=sys.stderr)
            return 2
        for package_dir in find_package_dirs(root):
            relative = package_dir.relative_to(root)
            if not relative.parts:
                print(f"skip default package: {package_dir}")
                continue
            package = ".".join(relative.parts)
            if not is_valid_package_name(package):
                print(f"skip invalid package name: {package} ({package_dir})")
                continue
            target = package_dir / "package-info.java"
            if target.exists():
                already_present += 1
                continue
            print(f"create: {target}")
            if not args.dry_run:
                with target.open("w", encoding="utf-8", newline="\n") as handle:
                    handle.write(TEMPLATE.format(package=package))
            created += 1

    print(f"created: {created}, already present (ignored): {already_present}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
