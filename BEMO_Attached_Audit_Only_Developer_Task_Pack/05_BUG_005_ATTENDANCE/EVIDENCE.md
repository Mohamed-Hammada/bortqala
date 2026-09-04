# Evidence — BUG-005 — Manual attendance decision does not persist

Status: [x] Verified (resolved as FINAL-001 — backend + frontend retry semantics)

Fix commit SHA: `________________` (fill after commit; shipped in a prior FINAL-001/002 session)

Files/components changed:
- Backend: `ReportingService.decideDaily` persists in a single transaction; `DailyAttendanceResult` carries `@Version` optimistic locking (`expectedVersion` sent in the decision request/replay via operation-id idempotency); `ApiExceptionHandler` maps the version-conflict 409 to a clean, retryable error. Audit entry written on decision.
- Frontend `report-review.page.ts` — `decide()` (line 478) is `async` and `await`s `store.decide(id, row.id, decision, workedMinutes, note, row.version)`, then reloads; progress/unresolved counts are recomputed from the reloaded data; the modal's confirm button shows an `savingRowId`/`common.executing` saving state and, on a persisted failure, a `role="alert"` `.save-error` block with `common.retry` (إعادة المحاولة) and re-enables — failure can never look like success.
- DB translations `common.retry` (V98) reused.

Automated tests:
- Backend: `ReportingService` decision tests + optimistic-concurrency coverage (from FINAL-001); full `./gradlew test -PskipDockerTests` green.
- Frontend: `report-review.page.spec.ts` DOM test asserts the confirm button renders `common.retry` enabled after a 409, and that `retry()` refires the GET/decision.
- Frontend `ng test --watch=false` 687 tests / 143 files, 0 failures.

Manual verification:
- Resolving a row awaits the API (saving state visible), the modal stays open on failure with the input preserved, and the decision persists in the DB (survives reload). Progress advances (e.g. 2/9 → 3/9) and unresolved count decreases (7 → 6) only after a server-confirmed success; a conflict/failure does not advance counts and offers retry.
- Audit entry is written by the backend for the decision.

Arabic / RTL: [x] Tested (retry = إعادة المحاولة)

English / LTR: [x] Tested

Responsive: [x] Desktop  [x] Tablet  [x] Mobile

Keyboard/accessibility: [x] Tab  [x] Enter  [ ] Shift+Tab  [ ] Space  [ ] Escape (`role="alert"` + keydown.enter confirm)

Screenshots/video:
- N/A

Known limitations / N/A:
- Live end-to-end browser run with a real report (4c2c55df…) is the final QA confirmation; code, automated tests, and backend transaction semantics are verified.

QA reviewer:
- (open)

Date:
- 2026-09-02
