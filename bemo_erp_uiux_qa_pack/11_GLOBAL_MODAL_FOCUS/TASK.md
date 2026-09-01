# TASK 11 — Global Modal/Dialog Behavior

Priority: P1
Status: ☑ Verified

## Required
1. Opening a dialog moves focus to the first logical control.
2. Tab is trapped within the dialog.
3. Shift+Tab works.
4. Escape closes only the topmost applicable overlay.
5. Closing restores focus to the trigger.
6. Background content is not keyboard reachable while modal is open.
7. Global shortcuts are suppressed while modal is open.
8. Nested confirmation dialogs have deterministic ownership.
9. Loading dialogs cannot be closed into an invalid state.
10. Screen readers receive dialog title and description.
