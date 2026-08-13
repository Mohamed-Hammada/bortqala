#!/usr/bin/env python3
"""Append one bilingual translation to an id-less Liquibase seed CSV safely."""

from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from pathlib import Path

BE_ROOT = Path(__file__).resolve().parents[1]
CATALOG = BE_ROOT / "src" / "main" / "resources" / "db" / "changelog"
FILES = CATALOG / "data" / "insert" / "files"
HEADER = ["translation_key", "locale", "text_value"]


def existing_pairs() -> set[tuple[str, str]]:
    pairs: set[tuple[str, str]] = set()
    for path in CATALOG.rglob("*.csv"):
        with path.open(encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle, delimiter=";")
            fields = set(reader.fieldnames or [])
            key_column = "translation_key" if "translation_key" in fields else "key" if "key" in fields else None
            if not key_column or "locale" not in fields:
                continue
            for row in reader:
                pairs.add(((row.get(key_column) or "").strip(), (row.get("locale") or "").strip()))
    return pairs


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", required=True, help="New id-less CSV filename under data/insert/files")
    parser.add_argument("--key", required=True)
    parser.add_argument("--ar", required=True, help="Arabic translation")
    parser.add_argument("--en", required=True, help="English translation")
    args = parser.parse_args()

    target = (FILES / args.file).resolve()
    if target.parent != FILES.resolve() or target.suffix.lower() != ".csv":
        parser.error("--file must be a CSV filename directly under data/insert/files")
    key = args.key.strip()
    valid_key = key and all(part and part.replace("_", "").replace("-", "").isalnum() for part in key.split("."))
    if not valid_key:
        parser.error("--key must contain dot-separated letters, numbers, underscores, or hyphens")
    if not args.ar.strip() or not args.en.strip():
        parser.error("Both translations must be non-empty")

    pairs = existing_pairs()
    duplicates = [(key, locale) for locale in ("ar-EG", "en-US") if (key, locale) in pairs]
    if duplicates:
        print(f"Refusing duplicate catalog entries: {duplicates}", file=sys.stderr)
        return 1

    previous = target.read_bytes() if target.exists() else None
    if target.exists():
        with target.open(encoding="utf-8-sig", newline="") as handle:
            if next(csv.reader(handle, delimiter=";"), []) != HEADER:
                print(f"{target.name} is not an id-less translation CSV with header {HEADER}", file=sys.stderr)
                return 1
    try:
        with target.open("a", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, delimiter=";", lineterminator="\n")
            if previous is None:
                writer.writerow(HEADER)
            writer.writerow([key, "ar-EG", args.ar.strip()])
            writer.writerow([key, "en-US", args.en.strip()])
        result = subprocess.run(
            [sys.executable, str(BE_ROOT / "tools" / "check-translation-catalog.py")],
            cwd=BE_ROOT,
            check=False,
        )
        if result.returncode:
            raise RuntimeError("translation catalog validation failed")
    except Exception:
        if previous is None:
            target.unlink(missing_ok=True)
        else:
            target.write_bytes(previous)
        raise

    print(f"Added {key!r} in ar-EG and en-US to {target.relative_to(BE_ROOT)}")
    print("Register this CSV in a new Liquibase loadData changeset after v228; omit the id column.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
