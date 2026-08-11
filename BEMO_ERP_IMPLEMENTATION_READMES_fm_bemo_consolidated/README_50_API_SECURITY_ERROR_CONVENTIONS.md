# API, Security and Error Conventions

> **Implementation package basis**
> - Repository: `Mohamed-Hammada/bortqala`
> - Branch: `fm_bemo_consolidated`
> - Verified branch HEAD while preparing this package: `aa3f940cca0119d7f523e03e3fd317fb72684cf3`
> - Business source: `SOURCE_ROADMAP.md`
> - Constraint: deterministic/non-AI implementation only
> - Rule: **extend existing code; do not create parallel ERP logic**
>
> File labels used below: `[EXISTS]` = verified in branch, `[MODIFY]` = existing file to change, `[NEW]` = proposed addition, `[VERIFY IN IDE]` = developer must locate exact existing convention before creating anything.


## Command request convention

For material state changes use request body fields (or current branch equivalent):

```json
{
  "operationId": "uuid",
  "expectedVersion": 12,
  "reason": "required only for override/reversal/reopen",
  "...businessFields": "..."
}
```

Do not accept authenticated `actorId` from browser as authority; resolve actor from security context.

## Response convention

Return source ID/status/version, generated references and next actions:

```json
{
  "id": 1001,
  "status": "POSTED",
  "version": 13,
  "operationId": "...",
  "journalId": 9002,
  "allowedActions": ["REVERSE"],
  "warnings": []
}
```

## Error map

| HTTP | Code | Example |
|---|---|---|
| 400 | VALIDATION_ERROR | missing line/invalid quantity |
| 403 | FORBIDDEN_ACTION | role lacks posting permission |
| 404 | DOCUMENT_NOT_FOUND | source id unavailable in scope |
| 409 | VERSION_CONFLICT | expectedVersion stale |
| 409 | ILLEGAL_STATE | approve CLOSED document |
| 409 | OPERATION_ID_REUSED | same ID, different payload |
| 409 | BUSINESS_BLOCKER | unresolved match/attendance/budget blocker |
| 422 | RULE_VIOLATION | deterministic business rule failure if current API uses 422; otherwise use current standard |

Follow the repository's existing global exception format. Do not create different JSON errors in every controller.

## Authorization

Minimum action permissions conceptually:
`VIEW`, `CREATE`, `EDIT_DRAFT`, `SUBMIT`, `APPROVE`, `EXECUTE`, `POST`, `PAY`, `REVERSE`, `REOPEN`, `OVERRIDE`.
Map these to existing role/authority infrastructure. Frontend menu/route guards are not sufficient.

## Data exposure

- beneficiary/bank snapshots: mask in ordinary views;
- do not log secrets/full banking values in audit/error logs;
- attachment access must inherit source-document permission;
- export APIs apply the same filters/tenant/app scope as list APIs;
- deterministic “recommendation” endpoint must expose formulas/inputs, never hidden scoring logic.

## Pagination/filtering

All exception/workbench queues need server pagination/filtering by state/date/party/site/warehouse/etc. Do not fetch entire ERP tables into Angular and filter client-side.

## Compatibility

Where current API exists, prefer additive DTO fields/new endpoints first. Deprecate old direct-pay/start/complete shortcuts only after the new flow passes E2E and frontend migration. Document deprecation in code/README; do not break the demo abruptly.
