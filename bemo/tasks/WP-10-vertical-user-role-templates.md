# WP-10 — Vertical-Aware User Creation & Job Role Templates
**Priority:** 🟠 · **Owner:** Full-stack dev F · **Depends on:** — · **Effort:** 3–4 days
**Liquibase:** V-number from coordinator (template tables + seed rows).
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §19

## Business goal
When adding a user, admin should see menus/roles matching the company's vertical (medical clinic ≠ factory). Today `users.page.ts:91` uses a static flat `USER_MENU_OPTIONS` list for everyone, and only 2 policy groups per vertical are provisioned.

## Backend steps
1. `GET /api/v1/auth/users/menu-options`: move the menu catalog server-side; response items `{id, labelKey, groupKey, verticalTags[], enabled}` where `enabled` = tenant feature flags active (`TenantFeatureService`). Fallback contract: FE keeps constant if endpoint fails.
2. Template tables: `user_role_templates` (id, app_id NULL=global, vertical, code, name_key, menu_ids text[], permission_prefixes text[], sort_order) seeded idempotently per vertical:
   - MEDICAL: Doctor, Nurse, Pharmacist, Lab Tech, Radiologist, Insurance Officer, Clinic Cashier
   - MANUFACTURING: Plant Supervisor, QC Inspector, Storekeeper, Maintenance Planner, Production Planner
   - RETAIL: Cashier, Merchandiser, Van-Sales Rep, Branch Manager
   - CIVIL: Site Engineer, Quantity Surveyor, Subcontractor Coordinator
   - SERVICES: Consultant, Support Agent · GENERAL: Accountant, HR Officer, Purchasing Officer
3. `GET /api/v1/auth/users/role-templates?vertical=` returns templates for tenant vertical (+GENERAL always).
4. Names via translation keys `users.template.<code>` — CSV rows both locales.

## Frontend steps
1. Users page fetches menu-options (fallback to constant) and role-templates; add "Job template" select in add-user dialog; selecting pre-checks matching menus + suggests matching policy group — everything stays manually editable.
2. Disabled menus render grayed with tooltip `users.menuNotEnabledForVertical`.
3. Tests: template applies expected checkbox set; fallback path works when endpoint errors; disabled menu unclickable.

## Acceptance Criteria (QA sign-off)
- [x] **AC-1** MEDICAL tenant's user dialog shows only medical-relevant menus enabled; manufacturing-only menus grayed with translated tooltip. ✅ (`.feature-locked` + `users.menuNotEnabledForVertical` tooltip, DOM-tested in users.page.spec.ts)
- [x] **AC-2** Choosing "Pharmacist" checks exactly the seeded pharmacy menu set and selects Pharmacy policy group; admin can still untick anything before save. ✅ (template apply merges menus union-style + auto-selects suggested groups; fully editable after — spec-covered)
- [x] **AC-3** Endpoint failure falls back to today's full static list with zero UI breakage (spec proves it). ✅ (computed falls back wholesale to static USER_MENU_OPTIONS when endpoint empty/unavailable)
- [x] **AC-4** GENERAL templates available to every vertical; saved users persist correct allowed_menus; existing users unaffected by migration. ✅ (service always unions GENERAL; V351 is additive seed)
- [x] **AC-5** Template names appear in Arabic on ar-EG locale from DB rows (not code constants); DoD gates pass. ✅ (V351 translations 29 keys × ar/en; gates: BE 822/197/0, FE 490/101/0, i18n 4679, hardcoded 0, error-codes 601/601, catalog 14,084 PASS)
