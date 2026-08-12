# BEMO ERP — Remaining Work

**Reviewed HEAD:** `8595cfa1b600f2bb4ac39fa52d32debcca5cb2ce`

The detailed line-by-line unfinished status is maintained in:

`BEMO_ERP_IMPLEMENTATION_READMES_fm_bemo_consolidated/IMPLEMENTATION_CHECKLIST.md`

Highest-priority remaining work:

1. Replace placeholder zero-vs-zero subledger reconciliation with real GL/subledger calculations.
2. Make versioned payroll inputs/components authoritative instead of client-supplied regular payroll totals.
3. Remove/restrict Procurement synthetic `ITEM-1` fallback and preserve source-line traceability.
4. Make Manufacturing release reserve materials; consume only through actual issue documents.
5. Separate Sales delivery creation, allocation/pick, shipment and delivery with partial quantities.
6. Enforce inventory availability and concurrency-safe reservation.
7. Replace hardcoded About build metadata with build/CI metadata.
8. Runtime-accept Web Push and admin notification targeting/security.
9. Verify/consolidate the existing Support/Feedback backend contract.
10. Remove tracked `.bemo-*-backup-*` directories and finish docs cleanup.
