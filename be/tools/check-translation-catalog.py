#!/usr/bin/env python3
"""Fail fast on translation CSV defects before Liquibase starts."""

from __future__ import annotations

import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "db" / "changelog"
KEY_COLUMNS = ("translation_key", "key")
VALUE_COLUMNS = ("text_value", "text", "value")
LOCALES = {"ar-EG", "en-US"}


def main() -> int:
    ids: dict[str, str] = {}
    pairs: dict[tuple[str, str, str], tuple[str, str]] = {}
    errors: list[str] = []
    rows_seen = 0
    dynamic_ids = 0
    key_locales: dict[tuple[str, str], set[str]] = {}

    for path in sorted(ROOT.rglob("*.csv")):
        relative = path.relative_to(ROOT).as_posix()
        with path.open(encoding="utf-8-sig", newline="") as handle:
            reader = csv.DictReader(handle, delimiter=";")
            headers = set(reader.fieldnames or [])
            key_column = next((name for name in KEY_COLUMNS if name in headers), None)
            value_column = next((name for name in VALUE_COLUMNS if name in headers), None)
            if not key_column or "locale" not in headers or not value_column:
                continue
            for line_number, row in enumerate(reader, start=2):
                rows_seen += 1
                location = f"{relative}:{line_number}"
                has_id_column = "id" in headers
                row_id = (row.get("id") or "").strip() if has_id_column else ""
                key = (row.get(key_column) or "").strip()
                locale = (row.get("locale") or "").strip()
                value = (row.get(value_column) or "").strip()
                scope = (row.get("app_id") or "DEFAULT").strip() or "DEFAULT"
                if None in row:
                    errors.append(f"{location}: extra column(s), usually an unquoted semicolon: {row[None]}")
                if not key or not value:
                    errors.append(f"{location}: key and value must be non-empty")
                if locale not in LOCALES:
                    errors.append(f"{location}: unsupported locale {locale!r}")
                if has_id_column:
                    if not row_id:
                        errors.append(f"{location}: legacy id column must not contain blank values")
                    elif row_id in ids:
                        errors.append(f"{location}: duplicate translation id {row_id!r}; first at {ids[row_id]}")
                    else:
                        ids[row_id] = location
                else:
                    dynamic_ids += 1
                pair = (scope, key, locale)
                if pair in pairs:
                    # A later file may intentionally redeclare a key with the
                    # identical text as a delta backfill for databases that
                    # already applied the earlier changeset (e.g. V251). Only
                    # a conflicting value is a defect.
                    if value != pairs[pair][1]:
                        errors.append(
                            f"{location}: duplicate key/locale {pair!r} with differing text; "
                            f"first at {pairs[pair][0]}"
                        )
                else:
                    pairs[pair] = (location, value)
                key_locales.setdefault((scope, key), set()).add(locale)

    for (scope, key), locales in sorted(key_locales.items()):
        missing = LOCALES - locales
        if missing:
            errors.append(f"{scope}:{key}: missing locale(s) {', '.join(sorted(missing))}")

    if errors:
        print(f"translation catalog: {rows_seen} rows | {len(errors)} defect(s)")
        for error in errors:
            print(f"  {error}")
        return 1
    print(f"translation catalog: {rows_seen} rows | {dynamic_ids} database-generated ids | bilingual key/locale pairs unique | PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
