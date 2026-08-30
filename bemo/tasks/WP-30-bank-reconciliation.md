# WP-30 — Bank Statement Import & Reconciliation Assist
**Priority:** 🟠 · **Owner:** Backend dev C · **Depends on:** — · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §17B

## Business goal
Accountant downloads bank statement (CSV/MT940-ish Excel) → import → system suggests matches against open receipts/payments/journal expectations → accountant confirms; unmatched stay as bank-only lines. Kills month-end pain.

## Backend steps
1. Tables: `bank_accounts` (name, bank, iban, currency, gl_account_code) · `bank_statement_imports` (batch header: file, checksum idempotent, counts, status) · `bank_statement_lines` (value_date epochMillis, description, debit/credit amount, running_balance nullable, status UNMATCHED|MATCHED|IGNORED, matched_ref_type/id NULL).
2. Import via SmartImport-style validation (dup checksum safe re-import). Suggestion engine v0 (deterministic, no ML): score = exact amount ± date window (±5d configurable) + text contains party name/reference number; return top-3 candidates per line with scores.
3. Confirm endpoint applies match by creating the missing side (e.g., customer receipt) OR linking an existing open document — both paths audited; ignore allowed with reason.
4. Codes: `BANK_*` family (~8).

## Frontend steps
1. Finance → Banks feature extension: import wizard (template download), statement grid grouped by date with match-quality chips (green auto ≥90, amber review), side panel showing candidate documents with one-click Confirm / Ignore / create-manually shortcut.
2. Keys `finance.bank*` (~18).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Same file imported twice yields zero duplicate lines (checksum test).
- [x] AC-2 Fixture of 20 lines: exact-amount+date+name lines auto-score ≥90 and confirm creates correct receipt in ONE click; ambiguous line shows 3 ranked candidates.
- [x] AC-3 Confirm is transactional: crash between confirm and ledger write leaves no half state (integration rollback test); ignored lines never post.
- [x] AC-4 Running balance column optional — files without it import fine; debit/credit signs land correctly per account perspective.
- [x] AC-5 Unmatched aging view shows >30-day unmatched totals per bank.
