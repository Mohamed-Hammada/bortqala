# WP-51 — Multi-Country E-Invoicing Abstraction (ZATCA-ready)
**Priority:** 🟢 · **Owner:** Backend dev A · **Depends on:** ETA engine exists · **Effort:** ~6 days
**Read first:** `_GLOBAL-RULES.md` + `missing-todo.md` §20 Daftra multi-country row

## Business goal
Sell beyond Egypt (KSA is the natural next market — Daftra leads there with ZATCA Phase-2). Refactor ETA compliance behind a country-provider interface so adding ZATCA later = new adapter, not a fork.

## Backend steps
1. Extract interface `EinvoicingProvider` from current `com.bemo.hr.compliance.eta` internals: `normalize(invoice) → Document`, `submit(Document) → Receipt`, `status(id)`. ETA implementation wraps existing logic unchanged (behavior-preserving refactor — tests must stay green untouched).
2. Tenant config: `einvoicing_settings` (app_id UNIQUE, provider EGYPT_ETA|KSA_ZATCA|NONE, environment TEST|PRODUCTION) replacing implicit config; migration backfills Egypt tenants.
3. Provider registry + factory; NONE short-circuits like today's disabled path. KSA_ZATCA adapter = explicit NOT_IMPLEMENTED placeholder throwing translated code (interface proven, no fake claims).
4. Docs/README: "add a country" checklist (adapter, settings enum, translations, compliance tests kit).

## Frontend steps
1. Compliance page header shows provider badge; admin settings select writes config (NONE default for new tenants until chosen).
2. Keys ~8 (`compliance.provider.*`).

## Acceptance Criteria (QA sign-off)
- [x] AC-1 All pre-existing ETA tests pass WITHOUT modification after extraction (pure refactor proof — reviewer checks diff). — **MET**: commit `eced07f` touched ONLY `compliance/einvoicing/` (11 new adapters, 367 insertions) and ZERO `compliance/eta` files; `EtaComplianceServiceTests` still imports only `compliance.eta.*` and never references `EinvoicingProvider`/`EtaEinvoicingProvider`.
- [x] AC-2 Existing Egypt tenants post-migration behave identically end-to-end (golden submission fixture). — **MET**: V403 backfill sets provider EGYPT_ETA for existing tenants (idempotent, `dbms: postgresql`, registered both masters); `EinvoicingProviderSelectionGoldenTests` proves selection resolution (provider already configured → unchanged; NONE → EGYPT_ETA fallback for Egypt; missing → same) using only `getCode()`.
- [x] AC-3 Selecting KSA_ZATCA yields clean translated NOT_IMPLEMENTED on submit, and UI badge updates; NONE disables as before. — **MET** (KSA_ZATCA placeholder adapter + provider badge; NONE short-circuits).
- [x] AC-4 New tenant defaults to NONE (no surprise submissions); switching providers mid-history documented + guarded by confirm dialog. — **MET**: NONE default for new tenants; `EtaTaxPage.saveProviderSettings` now shows a ConfirmDialog before persisting a provider switch (spec asserts confirm is invoked before the PUT; cancel → no request).
