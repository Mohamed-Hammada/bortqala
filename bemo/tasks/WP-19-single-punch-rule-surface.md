# WP-19 — Missed-Punch Per-Category Auto-Rule Surface
**Priority:** 🟠 · **Owner:** Frontend dev E · **Depends on:** — · **Effort:** ~1 day
**Read first:** `_GLOBAL-RULES.md`

## Business goal
Original ask: "user-defined categories where ≥1 fingerprint/day = attended." Backend ALREADY supports this — a category owns `singlePunchCounts` (one punch proves non-blocking presence when true; otherwise blocking SINGLE_PUNCH exception). Gap is only a discoverable first-class toggle in category settings instead of hidden configuration.

## Current state
`be/src/main/java/com/bemo/hr/reporting/application/AttendanceExceptionService.java` (`singlePunchScore`, :185-186) · backend skill invariant: *"A single punch proves non-blocking presence only for categories with `singlePunchCounts=true`"*.

## Steps
1. Verify the category API exposes `singlePunchCounts` read/write (add if missing — tiny BE change + column check).
2. Categories page: per-category switch "البصمة الواحدة تعتبر حضور / Single punch counts as attended" with explanatory hint text; save follows existing category save flow.
3. Report-review hint: where SINGLE_PUNCH exceptions render, show rule state chip ("auto-attended by rule" vs "blocking exception").
4. Keys: `categories.singlePunchCounts`, `categories.singlePunchCountsHint`, `review.ruleAutoAttended`, `review.ruleBlocking` + CSV.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Toggle persists per category and survives reload (API round-trip test). — **MET** (`singlePunchCounts` read/write exposed; category API round-trip tests).
- [x] AC-2 With rule ON, a 1-punch day no longer appears as blocking exception for that category; with OFF it does (integration fixture both ways). — **MET** (ONE_PUNCH bulk-accept + `AttendanceExceptionService.singlePunchScore`; both-state fixture tests).
- [ ] AC-3 Rule-state chip renders correctly on review rows in both locales; gates green. — **PARTIAL**: `report-review.page.html:378-382` renders only the `review.ruleBlocking` chip on undecided SINGLE_PUNCH rows; `review.ruleAutoAttended` is defined in i18n but never rendered (no dual-state chip).
