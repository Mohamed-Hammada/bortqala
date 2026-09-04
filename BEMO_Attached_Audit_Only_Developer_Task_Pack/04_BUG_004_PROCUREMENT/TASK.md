# BUG-004 — Procurement exposed but disabled

Priority: **CRITICAL**

/trade/procurement exposes a New Purchase Order action but the module is disabled.

Acceptance:
- [x] Procurement is genuinely enabled end-to-end OR clearly unavailable.
- [x] Supplier selection works.
- [x] Order lines work.
- [x] Approval/status lifecycle works.
- [x] Partial receipts work.
- [x] Invoice matching works.
- [x] Accounting posting works.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.