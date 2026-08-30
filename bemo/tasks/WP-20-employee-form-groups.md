# WP-20 — Employee Form Grouped Sections + Layout Preview
**Priority:** 🟠 · **Owner:** Full-stack dev F · **Depends on:** — · **Effort:** ~2 days
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Original complaint: add-new-employee form crams ~20–30 dropdown lists flat — confusing. Want fields organized into named groups where admin takes whole groups or partial ones, with layout preview.

## Current state
✅ **DONE 2026-08-27** — `fe/src/app/features/employees/employees.page.ts` exposes `FORM_GROUPS` (7 groups: identity/job/schedule/salary/contracts/biometric/dates) driving both the collapsible accordion form and the preview drawer (single source). Core FE-only, zero API change. V362 `employee_form_groups_translations` (19 keys × ar/en incl. `employees.group.invalidFields`) registered in `next.changelog-master.yaml`.

## Steps
1. Define group metadata array in the feature (identity / job & category / schedule / salary / contracts / biometric mapping …): `{key, titleKey, fieldIds[]}` — pure frontend reorganization v1, zero API change. ([x] done)
2. Render as collapsible accordion cards (first two expanded); required-field dots visible even when collapsed. ([x] done — required/invalid count badge shown on collapsed headers)
3. "Preview layout" button → read-only summary drawer showing exactly which groups/fields will appear, driven by the same metadata (single source). ([x] done — `previewFields()` respects salary visibility gating for 1:1 parity)
4. Optional admin setting (localStorage v1) to remember collapsed preferences per user. (deferred — out of v1 scope)
5. Keys: `employees.group.*` titles (~8) + `employees.previewLayout` + CSV. ([x] done — V362 CSV + i18n fallbacks)

## Tests
`employees.page.spec.ts` WP-20 block asserts: 7-group metadata, first-two-expanded default, toggle flip, required-field counts, preview toggle, AC-1 reachability (every non-version control covered by a group), AC-2 auto-expand of collapsed groups on invalid submit + invalid-badge state, AC-3 salary-visibility parity in preview. Full `ng test`: **553/553 across 113 files**. `check:i18n` **5,182 keys**; `check:hardcoded` **0 candidates in employees files** (15 pre-existing elsewhere from other in-progress WPs); `ng build` green.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Form renders as ≤8 titled groups; every existing field still reachable (snapshot test guards against dropped form controls).
- [x] AC-2 Collapsed group hides its fields but validation still blocks submit with error focused into the offending group (auto-expand on failed submit).
- [x] AC-3 Preview drawer mirrors real visibility 1:1 (same metadata drives both — spec proves equality).
- [x] AC-4 RTL/LTR layout intact; keyboard navigation through groups works; gates pass.
