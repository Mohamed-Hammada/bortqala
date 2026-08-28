# WP-42 — Growth Pack: Loyalty Points, Memberships/Subscriptions, Referrals
**Priority:** 🟢 · **Owner:** Full-stack F · **Depends on:** — · **Effort:** ~8 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Daftra gaps

## Business goal
Retail/gym/beauty tenants (Daftra's core base) expect: earn points per spend, sell recurring memberships with auto-renewal billing, and reward referrals. One pack, three features sharing the customer-wallet concept.

## Backend steps
1. Loyalty: `loyalty_accounts` (party FK, points balance cached + evidence rows `loyalty_ledger` EARN/REDEEM/EXPIRE with rule snapshot); rules: points per 100 EGP configurable, manual adjust (admin) audited; redeem at POS/sales as discount line (max % cap); expiry job by earn date + months config.
2. Memberships: `membership_plans` (name, price, period_days, grace_days, auto_renew default true) · `member_subscriptions` (party FK, plan FK, start_date, current_period_end, status ACTIVE|GRACE|EXPIRED|CANCELLED); renewal job: within grace create renewal invoice (draft→auto-post if property says so); access check endpoint for door integration later.
3. Referrals: `referrals` (referrer_party, referred_party unique, status REGISTERED|FIRST_PURCHASE|REWARDED, reward_points) → first purchase webhook from sales posting rewards both sides (configurable split).
4. Codes families `LOYALTY_* MEMBERSHIP_* REFERRAL_*` (~12).

## Frontend steps
1. Party detail: wallet card (points balance+history), memberships list with next-renewal badge; POS/sales tender adds "redeem points" input capped server-side.
2. Settings→Loyalty/Memberships: rule editors; members expiring-soon list; referrals report (top referrers).
3. Keys ~26 across three.

## Acceptance Criteria (QA sign-off)
- [x] AC-1 Spend 500 @ rule 1pt/100 → +5 points ledger row with rule snapshot; redeem 200 of them at cap 50% of a 300 sale → discount line exactly 150? NO — cap math: redemption value = points×point_value config; fixture asserts exact discount and remaining balance. — **MET**: `LoyaltyService` earn/redeem/adjust + ledger with rule snapshot implemented.
- [ ] AC-2 Points expire after configured months via job; expired rows EXPIRE-typed, balance never negative in any interleaving (concurrency pair test). — **PARTIAL**: `expiryMonths` rule field exists; NO expiry job and no concurrency pair test.
- [ ] AC-3 Membership renewing: invoice created in grace window; unpaid past grace → EXPIRED; cancel stops future invoices immediately. — **NOT MET**: `runRenewal` (`MembershipService.java:82-112`) is manual and only flips GRACE/EXPIRED/nextInvoiceDate — it does NOT create invoices.
- [ ] AC-4 Referral rewards fire ONCE per referred party at first posted sale (unique constraint + idempotent publisher). — **NOT MET**: `ReferralService.onFirstPurchase` (`:59-78`) is never called by sales posting (only tests/self-invoke).
- [ ] AC-5 All balances are derived-from-ledger verifiable (recompute endpoint/test matches cache). — **NOT MET**: no recompute endpoint found.
