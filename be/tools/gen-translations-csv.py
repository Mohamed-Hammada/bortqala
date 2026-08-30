#!/usr/bin/env python3
"""
WP-18 T-2: Translation CSV ID generator for Liquibase changesets (CLI wrapper).

Canonical implementation lives in gen_translations_csv.py (importable module);
this hyphenated entry point is the documented CLI (`python3 gen-translations-csv.py`).

Usage:
    echo '{"sales.foo": {"ar": "بار", "en": "Foo"}}' | python3 gen-translations-csv.py --version 358
    python3 gen-translations-csv.py --input translations.yaml --version 358 --target-csv src/main/resources/db/changelog/data/insert/files/20260826_v358_translations.csv
"""

from gen_translations_csv import main

if __name__ == '__main__':
    main()