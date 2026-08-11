# Non-AI Rules, Deterministic Recommendations and Scheduled Jobs

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Business outcome

Implement advanced automation without AI/LLMs. Every automated decision or recommendation must be reproducible from stored inputs, configuration and formulas and must be auditable.

## Rule types

Use typed configuration tables/services rather than free-form executable scripts initially.

Examples:
- approval amount thresholds;
- invoice match price/quantity/tax tolerance;
- overtime multiplier/cap;
- late/absence deduction;
- customer credit action;
- reorder/min-max;
- quality tolerance;
- budget blocking threshold;
- contractor invoice variance tolerance;
- manufacturing variance threshold.

Each rule needs where applicable:

```text
code
scope (app/branch/category/etc.)
effective_from/effective_to
version
priority
input parameters
active
created/approved metadata
```

A transaction/result stores rule version + actual inputs used.

## Deterministic recommendation contract

A recommendation is explanatory output, not autonomous authority. Suggested DTO:

```json
{
  "code": "INVOICE_QTY_VARIANCE_BLOCK",
  "severity": "BLOCKER",
  "messageKey": "procurement.match.qtyVariance.block",
  "formula": "abs(invoiceQty-receivedQty)/receivedQty*100",
  "inputs": {"invoiceQty":107,"receivedQty":100,"tolerancePct":5},
  "result": 7.0,
  "decision": "BLOCK"
}
```

Examples:
- invoice blocked: 7.2% > 5%;
- credit exceeded: exposure − limit = EGP 25,000;
- labor shortage: requested 18 − accepted 15 = 3;
- material shortage: required − available = 120 kg;
- budget shortage: requested 55,000 − available 40,000 = 15,000.

Do not generate prose with an LLM. Localized UI text comes from translation keys and numeric inputs.

## Scheduled jobs

Before creating a scheduler framework, search for existing scheduled tasks/configuration. Add jobs using the current Spring/project scheduling convention.

Candidate jobs:
- generate payroll calendar periods;
- generate workforce settlement periods;
- mark/queue overdue invoices and collection tasks;
- escalate approval tasks using existing due-date/escalation mechanism;
- refresh external FX hints on configured schedule;
- generate recurring journals;
- expire sales reservations;
- expire supplier contracts/price lists;
- create close precheck cache only if needed (close command still recomputes live blockers).

## Job execution safety

Every job must be idempotent. Recommended unique key:

`job_type + logical_period/entity + schedule_occurrence`

Rules:
1. same scheduled occurrence may run twice after restart without duplicate business documents;
2. failures are recorded with last error/attempt metadata according to current job infrastructure;
3. job-created business documents still use normal approval/posting rules;
4. a job may flag/prepare but must not bypass required human approval;
5. scheduler timezone must be explicit and follow application/business timezone configuration, not developer laptop timezone.

## Rule service ownership

Do not create one giant `RulesEngine` that knows every module. Prefer:
- small shared effective-version resolver;
- typed module policy services (`MatchTolerancePolicy`, `CreditPolicy`, `PayrollRuleResolver`, etc.);
- shared explanation DTO format.

## Frontend

Display deterministic recommendation cards inline with source data:
- severity;
- formula description;
- inputs;
- calculated result;
- configured threshold/version;
- allowed next action.

Users with permission may request approved override where business policy supports it. Override reason + approval reference is stored; original calculated result remains immutable.

## Tests

- rule effective-date boundary;
- two equal-priority overlapping rules rejected;
- exact tolerance boundary;
- explanation inputs reproduce result;
- job rerun creates no duplicate periods/journals/reservations;
- expired reservation released once;
- scheduler-created source still requires approval;
- external FX fetch failure preserves prior system rate and only marks hint refresh failure;
- timezone boundary at month/day transition.

## Junior developer execution order

1. Search for current settings/policy/scheduler code.
2. Define shared explanation DTO only.
3. Implement one typed policy (invoice match tolerance) with effective version and tests.
4. Update UI to show formula/inputs.
5. Apply to credit/budget/payroll/workforce/quality rules as each module is implemented.
6. Make one existing/needed periodic action idempotent.
7. Add operation record/unique key for scheduled occurrence if current scheduler lacks it.
8. Add monitoring/admin status using existing settings/admin conventions.
9. Never add AI libraries/services to satisfy a “recommendation” requirement.
