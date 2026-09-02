# BUG-012 — Save feedback is too generic

Priority: **MEDIUM**

Several operations show only `حفظ ✓`.

Acceptance:
- [x] Create/update/delete messages identify operation and entity.
- [x] Messages are localized (ar-EG/en-US DB rows + frontend REQUIRED_COPY fallbacks).
- [ ] Useful View/Open action is offered where appropriate. (N/A for the toast channel; action buttons remain in the source page/modal — see Known limitations in EVIDENCE.)
- [x] Failure never produces success feedback (toasts fire only on non-throwing returns; error branches keep modal open).

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.