# TASK 09 — Settings Save Semantics

Priority: P1/P2
Status: ☑ Verified

## Required
1. Simple toggles may use immediate save only if a visible saved state exists.
2. Complex forms use explicit Save/Cancel.
3. Dirty state is visible for complex forms.
4. Save button is disabled while saving.
5. Double-submit is impossible.
6. Failed save preserves the user's draft.
7. Successful save confirms persistence.
8. Cancel restores the last persisted state.
9. Leaving a dirty complex form prompts the user.
10. Save behavior is consistent across all Settings tabs.

## Deliverable
Document which components are immediate-save and which are explicit-save.
