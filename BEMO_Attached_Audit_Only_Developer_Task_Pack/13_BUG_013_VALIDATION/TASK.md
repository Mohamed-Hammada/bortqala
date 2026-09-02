# BUG-013 — Required-field validation lacks visible explanation

Priority: **MEDIUM**

Blank required workforce-category name focuses the field but provides no visible explanation.

Acceptance:
- [x] Localized inline error is visible.
- [x] aria-invalid is correct.
- [x] aria-describedby connects error/help text.
- [x] Long forms provide validation summary. (N/A — short single-entity modal)
- [x] Error clears after correction.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.