# BUG-008 — Worker daily rate does not inherit from category

Priority: **HIGH**

Selected category rate is 350 EGP but worker form defaults to 200 EGP.

Acceptance:
- [x] Category rate populates the worker form.
- [x] Overrides are allowed only when business rules permit.
- [x] Override is clearly labelled.
- [x] Saved worker uses effective rate.
- [x] Cost/settlement calculations use effective rate.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.