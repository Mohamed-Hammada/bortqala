# QA Rules

Use for QA, regression, and browser testing tasks.

## Method
1. Reproduce before changing code.
2. Identify exact expected vs actual behavior.
3. Trace UI -> API -> service -> persistence when necessary.
4. Preserve evidence while testing.
5. Do not mark a bug fixed without retesting the original reproduction path.

## Browser / E2E
Use the project's existing browser tooling and Playwright MCP when available.
Prefer focused flows over crawling unrelated areas.

## QA Report
When a report is requested, record issues as they are discovered:
- ID
- severity
- area
- preconditions
- steps
- expected
- actual
- evidence
- likely root cause
- status / verification

Separate implementation claims from verified evidence.
