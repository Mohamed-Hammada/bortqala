#!/usr/bin/env python3
"""Reject role names in backend annotations that are absent from RoleCode."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src" / "main" / "java"
ROLE_FILE = JAVA / "com" / "bemo" / "hr" / "shared" / "security" / "RoleCode.java"

enum_body = re.search(r"enum\s+RoleCode\s*\{(?P<body>.*?)\}", ROLE_FILE.read_text(encoding="utf-8"), re.S)
if not enum_body:
    raise SystemExit("FAIL: could not parse RoleCode.java")
roles = set(re.findall(r"\b[A-Z][A-Z0-9_]+\b", enum_body.group("body")))

references: dict[str, list[str]] = {}
for path in JAVA.rglob("*.java"):
    text = path.read_text(encoding="utf-8")
    for annotation in re.findall(r"@PreAuthorize\(\"(.*?)\"\)", text, re.S):
        for role in re.findall(r"has(?:Any)?Role\([^)]*?'([A-Z][A-Z0-9_]*)'", annotation):
            references.setdefault(role, []).append(str(path.relative_to(ROOT)))
        for group in re.findall(r"hasAnyRole\(([^)]*)\)", annotation):
            for role in re.findall(r"'([A-Z][A-Z0-9_]*)'", group):
                references.setdefault(role, []).append(str(path.relative_to(ROOT)))

unknown = sorted(set(references) - roles)
print(f"authorization contract: {len(roles)} declared roles | {len(references)} referenced roles | unknown: {len(unknown)}")
if unknown:
    for role in unknown:
        print(f"  {role}: {', '.join(sorted(set(references[role])))}")
    sys.exit(1)
print("PASS: every @PreAuthorize role exists in RoleCode")
