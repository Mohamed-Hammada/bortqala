# BUG-008 — Worker daily rate does not inherit from category

Priority: **HIGH**

Selected category rate is 350 EGP but worker form defaults to 200 EGP.

Acceptance:
- [ ] Category rate populates the worker form.
- [ ] Overrides are allowed only when business rules permit.
- [ ] Override is clearly labelled.
- [ ] Saved worker uses effective rate.
- [ ] Cost/settlement calculations use effective rate.

## Completion rule
The developer must not mark the index item complete until the acceptance checks above have been verified.