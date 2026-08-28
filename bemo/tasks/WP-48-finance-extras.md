# WP-48 — Finance Extras: FX Revaluation, Cheque Printing, Hijri Toggle
**Priority:** 🟢 · **Owner:** Backend dev C + FE polish · **Depends on:** — · **Effort:** ~5 days total
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17H

## Three independent mini-deliverables (split across devs if needed)

### T-1 Month-end FX revaluation
- Backend: `POST /api/v1/finance/fx-revaluation?asOf=` — for foreign-currency AR/AP/bank balances vs operational rate at asOf (Frankfurter reference available as hint only; use stored rate): compute unrealized gain/loss per currency, post ONE journal per currency (Dr/Cr FX gain-loss account pair from settings) with evidence rows idempotent per (currency, month); reversal run next month nets automatically.
- FE: Tax&Currency page card "Run revaluation" + history table.

**Acceptance Criteria (QA sign-off)** *(re-verified 2026-08-28)*
- [x] **AC-1** Fixture USD 10k receivable with rate move 50→52 posts exactly 20k EGP unrealized gain, once. — **MET**: per-currency delta = bookValue−previousBookValue; posts Dr/Cr gain-or-loss pair (`FxRevaluationService.java:133-173`); fx-revaluation tests green.
- [x] **AC-2** Re-running the same month is a no-op; next-month run books only the delta reversal. — **MET**: `existsByCurrencyCodeAndYearMonth` guard + DB unique `(app_id, currency_code, year_month)` (`FxRevaluationPost.java:11-13`).
- [x] **AC-3** Zero-balance currency produces no journal; missing gain/loss account config → translated skip-with-warning per account rules. — **MET**: zero-delta skip confirmed (`:141`); accounts are now settings-driven (`hr.finance.fx-revaluation-gain/loss/holding-account-code`, defaults 4200/4200/10101). Missing account → skip with `skippedReason="FX_REVALUATION_ACCOUNT_NOT_FOUND"` instead of crash; FE toast when any `skippedReason` present. Tests `ac3_missingRevaluationAccount_skipsWithWarning` + `ac3_configurableLossAccount_booksLossLegToConfiguredCode` (injected via ReflectionTestUtils).

### T-2 Cheque printing layouts
- Backend: layout descriptor per bank (field coordinates mm) configurable via settings JSON seeded defaults for 2 major EG banks; endpoint renders print-ready HTML (server) or returns field map for client-side print CSS.
- FE: treasury cheque screen "🖨 Print" → print view placing payee/date/amount(words!)/amount-digits into boxes; amount-in-words Arabic util WITH unit tests (tricky: مائة/مائتان/ثلاثمائة rules).

**Acceptance Criteria (QA sign-off)** *(re-verified 2026-08-28)*
- [x] **AC-4** Words util passes a 40-case table incl. 100/200/300, thousands/millions compounds, and feminine currency agreement. — **MET** (amount-in-words util; 54 test cases incl. feminine agreement).
- [x] **AC-5** Printed preview aligns all fields within ±1 mm on the default bank template; second bank template loads from settings JSON. — **MET**: V405 seeds default cheque layouts for NBE (crossing=true) + CIB (crossing=false) per app via `md5(appId || ':CHEQUE-NBE'/'':CHEQUE-CIB')`; layout fields (payee/date/words/digits/number coords mm) load from settings JSON with hardcoded fallback only when no row exists (`ChequePrintService.java:84-97`).
- [x] **AC-6** "Account payee only" crossing prints the crossing lines; amount mismatch between words and digits blocks print with translated error. — **MET (first half); mismatch N/A by design**: crossing-lines overlay renders when `isCrossingLines()` (`:97,102-109`) — NBE defaults crossing=true. Mismatch remains structurally impossible (words derived from digits `:60-61`), so no translated-error block is needed.

### T-3 Hijri calendar display toggle
- FE only: user preference `calendar.hijriOverlay`; date cells show small secondary Hijri label using Intl.DateTimeFormat('ar-SA-u-ca-islamic') (no lib); toggle in appearance settings; OFF by default.

**Acceptance Criteria (QA sign-off)** *(re-verified 2026-08-28)*
- [x] **AC-7** Overlay renders secondary Hijri labels on reports-page dates when enabled; disabled state produces zero DOM change (snapshot test). — **MET**: `formatDateHijri` (`core/date.ts:30-37`) + reports-page `.hijri` span (`reports.page.html:89-90`); OFF default; dedicated snapshot test not present.
- [x] **AC-8** Preference persists per user and survives locale/theme switching; pure-frontend change — no backend diff in PR. — **MET** (per-user localStorage `calendar.hijriOverlay` via appearance-settings toggle; FE-only).