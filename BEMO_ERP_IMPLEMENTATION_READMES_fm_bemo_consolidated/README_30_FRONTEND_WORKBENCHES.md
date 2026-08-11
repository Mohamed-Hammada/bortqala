# Frontend Cycle Workbenches — Angular Implementation

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Objective

Do not make users navigate isolated CRUD screens to understand a business cycle. Build workbenches that summarize current state, blockers, next action, required role, and stock/financial effect while keeping existing detail pages as the edit/execution surfaces.

## Existing frontend anchors

- `[EXISTS] fe/src/app/app.routes.ts` — root lazy routing/guards.
- `[EXISTS] fe/src/app/features/workforce/workforce.routes.ts`.
- `[EXISTS] Procurement`: `fe/src/app/features/trade/procurement/procurement.page.ts/html/scss/spec.ts`.
- `[EXISTS] Sales`: `fe/src/app/features/trade/sales/sales.page.ts/html/scss/spec.ts`.
- `[EXISTS] workforce page folders`: contractors, workers, labor-requests, manual-attendance, settlement-periods, advances, contractor-accounts.
- `[EXISTS] finance, approvals, audit-logs, fiscal-periods, manufacturing, operations features.

## Workbench response contract

Prefer backend aggregation endpoints rather than 15 Angular requests. Example DTO:

```ts
interface CycleWorkbenchCard {
  code: string;
  titleKey: string;
  count: number;
  amount?: number;
  currency?: string;
  severity: 'INFO'|'WARNING'|'BLOCKER';
  route?: string;
  requiredAction?: string;
}
```

Do not put business calculations only in TS. Backend provides authoritative counts/amounts and formulas where needed.

## Procurement workbench

Cards: requisitions awaiting approval, RFQs awaiting quotes, POs due/late, receipts pending inspection, match exceptions, invoices due for payment.

Implementation:
1. keep current `procurement.page.ts` as shell or current tab during migration;
2. extract existing large sections into child standalone components gradually;
3. add a workbench summary at top/route;
4. all cards link to filtered lists rather than duplicating records.

## Sales workbench

Orders credit-blocked, waiting stock, deliveries due, uninvoiced deliveries, overdue invoices, unallocated receipts.

## Payroll workbench

Input readiness, attendance blockers, current run state, deterministic prior-run variance, approval, journal posting, payment batch.

## Period close workbench

One screen using CloseChecklist API; PASS/WARNING/BLOCKER with drill-down.

## Shared action-state UX

Every detail page should receive from backend:

```json
{
  "status": "PENDING_APPROVAL",
  "version": 12,
  "allowedActions": ["CANCEL"],
  "blockingReasons": [{"code":"APPROVAL_REQUIRED","messageKey":"..."}],
  "links": {"approvalInstanceId":99}
}
```

Angular:
- disables/hides based on `allowedActions` for UX only;
- sends `expectedVersion` from current view and a newly generated `operationId` on command;
- on 409 version conflict, refetches and shows “record changed; review current state”;
- on duplicate succeeded operation, displays returned prior result normally;
- does not optimistically invent POSTED/PAID state before backend success.

## Dialog/form rules

- state-changing financial actions show impact preview;
- reversal/override/reopen require reason;
- save button disables while pending;
- Esc closes ordinary dialogs unless a critical execution is in progress according to shared modal behavior;
- long forms split into logical steps/tabs, but the server remains the source of validation;
- Arabic/English translation keys follow current translation system, never hard-code a parallel i18n mechanism.

## Frontend testing

For every new action test:
- permitted state shows button;
- blocked state shows reason;
- request includes operationId + expectedVersion;
- double click produces one client request or safe duplicate operation;
- 409 refetch path;
- server validation shown at field/business level;
- loading and retry;
- permission/menu guard plus backend 403 behavior is handled;
- RTL layout if Arabic is supported by current app.
