#!/usr/bin/env python3
"""CI regression gate: detect recurrence of reviewed anti-patterns.

Scans backend Java source files for patterns that were fixed during the
code review (BE-002 through BE-005). Exits non-zero if new violations
are found, so it can be used as a pre-commit hook or CI step.

Checks:
  - empty_catch: catch blocks with empty or comment-only bodies (BE-002)
  - field_autowired: @Autowired on non-constructor fields (BE-006)
  - missing_valid: @RequestBody without @Valid on DTOs with validation (BE-003)

Run from the be/ root: python3 tools/check-review-anti-patterns.py
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src")

def grep(pattern, src=None, extra_args=None):
    """Run grep and return (file, line_no, text) tuples."""
    cmd = ["grep", "-rn", "--include=*.java", "--exclude-dir=device-hub"]
    if extra_args:
        cmd.extend(extra_args)
    cmd.extend([pattern, src or SRC])
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
    hits = []
    for line in result.stdout.strip().split("\n"):
        if not line or ":" not in line:
            continue
        parts = line.split(":", 2)
        if len(parts) >= 3:
            hits.append((parts[0], int(parts[1]), parts[2].strip()))
    return hits

def check_empty_catches():
    """Find catch blocks where the body is empty or comment-only."""
    hits = grep(r"catch\s*\(")
    violations = []
    seen = set()
    for fpath, line_no, text in hits:
        key = (fpath, line_no)
        if key in seen:
            continue
        seen.add(key)
        try:
            with open(fpath, "r", encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
            # Find the opening { — could be on same line or next line
            brace_line_idx = line_no - 1  # 0-indexed
            found_brace = False
            for search_idx in range(brace_line_idx, min(brace_line_idx + 2, len(lines))):
                if "{" in lines[search_idx]:
                    found_brace = True
                    brace_line_idx = search_idx
                    break
            if not found_brace:
                continue
            # Check next 3 lines for }
            body_content = ""
            for check_idx in range(brace_line_idx + 1, min(brace_line_idx + 4, len(lines))):
                body_content += lines[check_idx]
            # Strip comments
            cleaned = re.sub(r'//[^\n]*', '', body_content)
            cleaned = re.sub(r'/\*.*?\*/', '', cleaned, flags=re.DOTALL)
            cleaned = cleaned.strip()
            if re.match(r'\}\s*$', cleaned):
                violations.append((fpath, line_no, "empty_catch"))
        except Exception:
            pass
    return violations

def check_autowired():
    """Find @Autowired followed by 'private' on the next line (field injection) — production only."""
    hits = grep(r"^.*@Autowired\s*$", src=os.path.join(ROOT, "src", "main", "java"))
    violations = []
    for fpath, line_no, _ in hits:
        try:
            with open(fpath, "r", encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
            if line_no < len(lines):
                next_line = lines[line_no].strip()
                if re.match(r"private\s+\w+", next_line) and "void" not in next_line:
                    violations.append((fpath, line_no + 1, "field_autowired"))
        except Exception:
            pass
    return violations

def check_missing_valid():
    """Find @RequestBody DTOs missing @Valid where the DTO has validation annotations."""
    # Step 1: find DTOs with validation annotations
    val_hits = grep(r"@(NotBlank|Size|Pattern|Email|NotNull|Min|Max|Positive|PositiveOrZero)\b")
    dto_has_val = set()
    for fpath, _, _ in val_hits:
        base = os.path.basename(fpath).replace(".java", "")
        dto_has_val.add(base)

    # Step 2: find @RequestBody without @Valid
    rb_hits = grep(r"@RequestBody\s+")
    violations = []
    for fpath, line_no, text in rb_hits:
        m = re.search(r"@RequestBody\s+(@Valid\s+)?(\w+)", text)
        if not m:
            continue
        has_valid = m.group(1) is not None
        if not has_valid and line_no > 1:
            try:
                with open(fpath, "r", encoding="utf-8", errors="replace") as f:
                    lines = f.readlines()
                if line_no >= 2 and "@Valid" in lines[line_no - 2]:
                    has_valid = True
            except Exception:
                pass
        if not has_valid and m.group(2) in dto_has_val:
            violations.append((fpath, line_no, f"missing_valid @RequestBody {m.group(2)}"))
    return violations

def main():
    violations = []
    violations.extend(check_empty_catches())
    violations.extend(check_autowired())
    violations.extend(check_missing_valid())

    if violations:
        print(f"\n{'='*70}")
        print(f"REVIEW ANTI-PATTERN GATE: {len(violations)} violation(s) found")
        print(f"{'='*70}")
        for fpath, line, detail in sorted(violations):
            rel = os.path.relpath(fpath, ROOT)
            print(f"  {rel}:{line}  {detail}")
        print(f"\nFix these violations before committing.")
        sys.exit(1)
    else:
        print("REVIEW ANTI-PATTERN GATE: 0 violations — PASS")
        sys.exit(0)

if __name__ == "__main__":
    main()
