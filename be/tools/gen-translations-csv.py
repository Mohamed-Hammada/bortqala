#!/usr/bin/env python3
"""
WP-18 T-2: Translation CSV ID generator for Liquibase changesets.

Input: YAML dict of {translation_key: {ar: "Arabic text", en: "English text"}}
Output: CSV rows with sequential IDs following the repo convention (vNNN-NNN-en/-ar)

Usage:
    echo '{"sales.foo": {"ar": "بار", "en": "Foo"}}' | python3 gen-translations-csv.py --version 358
    python3 gen-translations-csv.py --input translations.yaml --version 358 --target-be src/main/resources/db/changelog/data/insert/files/20260826_v358_translations.csv
"""

import argparse
import csv
import io
import os
import re
import sys
import yaml


def find_max_numeric_id(csv_path: str) -> int:
    """Scan an existing CSV for the highest numeric sequence number in translation IDs.

    IDs follow the pattern vNNN-NNN where NNN is numeric. Returns the max found,
    or 0 if the file doesn't exist or has no matching IDs.
    """
    max_num = 0
    if not os.path.exists(csv_path):
        return max_num
    with open(csv_path, 'r', encoding='utf-8') as f:
        for line in f:
            # Match v{digits}-{digits} at start of ID field
            m = re.match(r'^v(\d+)-(\d+)', line.strip())
            if m:
                num = int(m.group(2))
                if num > max_num:
                    max_num = num
    return max_num


def scan_existing_ids(csv_path: str) -> set:
    """Return all existing IDs from a CSV to detect duplicates."""
    ids = set()
    if not os.path.exists(csv_path):
        return ids
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f, delimiter=';', quotechar='"')
        for row in reader:
            if row and row[0].startswith('v'):
                ids.add(row[0])
    return ids


def generate_csv(translations: dict, version: int, start_seq: int = 1) -> str:
    """Generate CSV content with sequential IDs for each translation key.

    Args:
        translations: {key: {ar: "...", en: "..."}}
        version: Liquibase version number (for ID prefix)
        start_seq: Starting sequence number

    Returns:
        CSV string with semicolon delimiter and quoted fields.
    """
    output = io.StringIO()
    writer = csv.writer(output, delimiter=';', quotechar='"', quoting=csv.QUOTE_MINIMAL)
    writer.writerow(['id', 'translation_key', 'locale', 'text_value'])

    seq = start_seq
    for key, locales in sorted(translations.items()):
        for locale, locale_key in [('en', 'en'), ('ar', 'ar')]:
            text = locales.get(locale_key, '')
            row_id = f'v{version}-{seq:03d}-{locale}'
            writer.writerow([row_id, key, f'{locale}-EG' if locale == 'ar' else 'en-US', text])
        seq += 1

    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(description='Generate Liquibase translation CSV')
    parser.add_argument('--version', type=int, required=True, help='Liquibase version number (e.g. 358)')
    parser.add_argument('--input', type=str, help='Input YAML file (default: stdin)')
    parser.add_argument('--target-csv', type=str, help='Target CSV to scan for existing IDs')
    parser.add_argument('--start-seq', type=int, default=None, help='Override starting sequence')
    args = parser.parse_args()

    # Read input
    if args.input:
        with open(args.input, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f)
    else:
        data = yaml.safe_load(sys.stdin.read())

    if not data:
        print("ERROR: No translation data provided", file=sys.stderr)
        sys.exit(1)

    # Determine start sequence
    if args.start_seq is not None:
        start_seq = args.start_seq
    elif args.target_csv:
        max_existing = find_max_numeric_id(args.target_csv)
        start_seq = max_existing + 1
    else:
        start_seq = 1

    # Check for duplicate IDs
    if args.target_csv:
        existing_ids = scan_existing_ids(args.target_csv)
        new_ids = set()
        for i, key in enumerate(sorted(data.keys()), start=start_seq):
            new_ids.add(f'v{args.version}-{i:03d}-en')
            new_ids.add(f'v{args.version}-{i:03d}-ar')
        duplicates = existing_ids & new_ids
        if duplicates:
            print(f"WARNING: {len(duplicates)} duplicate IDs detected: {sorted(duplicates)[:5]}...", file=sys.stderr)

    csv_content = generate_csv(data, args.version, start_seq)
    print(csv_content, end='')


if __name__ == '__main__':
    main()
