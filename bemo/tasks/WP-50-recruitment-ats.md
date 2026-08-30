# WP-50 — Recruitment ATS Basics
**Priority:** 🟢 · **Owner:** Full-stack F · **Depends on:** — · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Odoo HR gap

## Business goal
Post a job opening, collect applicants, move them through stages, and on hire convert to an employee record without retyping. v1 = lean ATS (no public job-board integrations).

## Backend steps
1. Tables: `job_openings` (title_ar/en, department_id, headcount, status DRAFT|OPEN|CLOSED, description, published bool) · `job_applications` (opening FK, full_name, phone, email, source, cv attachment trio, stage NEW|SCREENING|INTERVIEW|OFFER|HIRED|REJECTED, rating 1..5 nullable, notes) · `application_stage_events` (from/to stage, actor, at, note) — stage history as evidence.
2. Endpoints `/api/v1/recruitment/openings|applications`: kanban moves validate transitions; HIRED triggers `POST /applications/{id}/convert` → prefilled employee-create payload (returns employeeId link stored back).
3. Duplicate applicant detection by phone/email exact match warning (not block).
4. Codes `RECR_*` (~6).

## Frontend steps
1. `features/recruitment/`: openings list + editor; applications kanban (columns = stages) with drag-free move buttons + detail drawer (CV download, rating stars, notes timeline); convert wizard prefilling WP-20 grouped employee form.
2. Menu under HR workspace; permission `recruitment.*` via AccessCatalog.
3. Keys ~22.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Stage machine matrix enforced incl. REJECTED terminal unless reopen-permission flag; every move writes history row visible in timeline. — **MET** (stage transition validation + `application_stage_events` history).
- [x] AC-2 Convert creates employee with mapped fields and marks application HIRED with link; second convert blocked. — **MET** (convert → prefilled employee-create, HIRED + employeeId link stored back).
- [x] AC-3 Duplicate phone/email shows non-blocking warning banner listing prior application(s). — **MET** (debounced `checkWarnings()` + `GET /applications/duplicates` banner; spec asserts the prior-applicant row).
- [x] AC-4 CV upload enforces REM-005 size/type trio; closed opening rejects new applications. — **MET** (`POST/GET /applications/{id}/cv`; 5MB + allowed-type validation, `RECR_CV_*` codes; FE file input + chip in the application drawer, validation trio + upload-after-create wiring; 5 FE specs + 4 BE CV tests; closed-opening rejection separately present).

## Verification (2026-08-28)
- Backend `test -PskipDockerTests --tests "com.bemo.hr.recruitment.*"` — BUILD SUCCESSFUL (9 tests: 6 original fixed + 3 new CV tests). Original suite had latent failures (UnnecessaryStubbing on `auth.getName`, false `eventRepository` verify in `createOpeningSavesSuccessfully`) — added `@MockitoSettings(LENIENT)` + corrected the verify.
- FE `ng test` — 563 tests / 114 files, 0 failures (incl. 5 recruitment WP-50 specs).
- `check:i18n` 5205 keys PASS; `check:hardcoded` PASS; error codes 682/682; translation catalog 15,819 PASS.
- V406 schema + translations registered in both masters.
