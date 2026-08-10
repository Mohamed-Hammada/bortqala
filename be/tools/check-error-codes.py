#!/usr/bin/env python3
"""CI gate (E-2): every backend exception code must have a DB translation row.

Scans src/main/java for BusinessRuleException/NotFoundException constructor
calls that carry an explicit machine code, then asserts each code appears in at
least one translations CSV under db/changelog/data/insert/files/ (a code shipped
without a row would make ApiExceptionHandler fall back to the raw message).

Usage (from be/): python3 tools/check-error-codes.py
"""
import glob
import os
import re
import sys

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
MAIN_SRC = os.path.abspath(os.path.join(BASE_DIR, "..", "src", "main", "java"))
INSERT_DIR = os.path.abspath(os.path.join(BASE_DIR, "..", "src", "main", "resources", "db", "changelog", "data", "insert", "files"))

CODE_RE = re.compile(
    r'(?:BusinessRuleException|NotFoundException)\(\s*"[^"]*"\s*,\s*"([A-Z][A-Z0-9_]{3,})"'
)
NOT_FOUND_RE = re.compile(r'new NotFoundException\([^;]{0,300}?"([A-Z][A-Z0-9_]{3,})"')
HELPER_RE = re.compile(r'\berror\(\s*"([A-Z][A-Z0-9_]{3,})"\s*,')

codes = set()
for path in glob.glob(os.path.join(MAIN_SRC, "**", "*.java"), recursive=True):
    with open(path, encoding="utf-8") as fh:
        src = fh.read()
    for match in CODE_RE.finditer(src):
        codes.add(match.group(1))
    for match in NOT_FOUND_RE.finditer(src):
        codes.add(match.group(1))
    for match in HELPER_RE.finditer(src):
        codes.add(match.group(1))

known = set()
for csv_path in glob.glob(os.path.join(INSERT_DIR, "*.csv")):
    with open(csv_path, encoding="utf-8-sig") as fh:
        for line in fh:
            parts = line.split(";")
            if len(parts) < 2:
                continue
            key = parts[1].strip()
            if key and not line.startswith("translation_key"):
                known.add(key)

missing = sorted(codes - known)
print(f"exception codes: {len(codes)} | with DB translation rows: {len(codes - set(missing))} | missing: {len(missing)}")
if missing:
    print("FAIL: codes without a translations CSV row:")
    for code in missing:
        print(f"  {code}")
    sys.exit(1)
print("PASS: every exception code has an ar-EG/en-US translation row")
