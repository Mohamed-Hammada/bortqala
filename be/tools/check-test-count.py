#!/usr/bin/env python3
"""CI regression gate: fail if the backend test counts drop below baseline.

Reads JUnit XML reports from build/test-results/test/*.xml (produced by
`./gradlew test`) and asserts the non-Docker baseline. Run from the be/ root.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESULT_DIR = os.path.join(BASE_DIR, "build", "test-results", "test")

MIN_TESTS = 281
MIN_SUITES = 58

reports = glob.glob(os.path.join(RESULT_DIR, "*.xml"))
if not reports:
    print(f"FAIL: no JUnit XML reports found under {RESULT_DIR}")
    print("      run `./gradlew test -PskipDockerTests` first")
    sys.exit(1)

tests = failures = errors = skipped = 0
for path in reports:
    root = ET.parse(path).getroot()
    tests += int(root.get("tests", 0) or 0)
    failures += int(root.get("failures", 0) or 0)
    errors += int(root.get("errors", 0) or 0)
    skipped += int(root.get("skipped", 0) or 0)

suites = len(reports)
print(
    f"backend tests: {tests} (min {MIN_TESTS}) | suites: {suites} "
    f"(min {MIN_SUITES}) | failures: {failures} | errors: {errors} | skipped: {skipped}"
)

ok = True
if tests < MIN_TESTS:
    print(f"FAIL: test count {tests} below baseline {MIN_TESTS}")
    ok = False
if suites < MIN_SUITES:
    print(f"FAIL: suite count {suites} below baseline {MIN_SUITES}")
    ok = False
if failures or errors:
    print("FAIL: test failures or errors present")
    ok = False

sys.exit(0 if ok else 1)
