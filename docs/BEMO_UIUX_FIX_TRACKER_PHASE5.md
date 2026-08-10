# BEMO ERP UI/UX Fix Tracker — Phase 5

Reviewed branch: `fm_bemo_consolidated`  
Reviewed commit: `75d6ffaaabfac4705224683b521cd2e41f70c587`

Status meanings:
- **IMPLEMENTED** = included by the Phase 5 patch.
- **VERIFY** = manual browser verification still required after build/deploy.
- **DONE** = mark only after the manual QA check passes.

| ID | Screen / scope | Issue | Fix | Patch status | Manual QA |
|---|---|---|---|---|---|
| UI5-01 | Global forms / Login / dialogs | Arabic UI fields visually align values to the left | Locale-level field alignment: RTL → right, LTR → left, while technical values keep LTR character order | IMPLEMENTED | VERIFY |
| UI5-02 | `/workforce/settlement-periods` | Workflow/state bar uses a light hard-coded surface and text loses contrast in Dark mode | Replaced with semantic BEMO surface/line/text tokens and readable step chips | IMPLEMENTED | VERIFY |
| UI5-03 | `/manufacturing/quality` | Last field can collide visually with the save/action bar; legacy drawer differs from product dialogs | Migrated legacy drawer to `app-modal-dialog`; form body and action row are separated | IMPLEMENTED | VERIFY |
| UI5-04 | `/manufacturing/production` | Two page tabs still use legacy pill styles | Migrated to shared `.app-tabs/.app-tab` | IMPLEMENTED | VERIFY |
| UI5-05 | `/fiscal-periods` | `السنة المالية` and create-year button are vertically misaligned; duplicate lightning decoration | Added aligned fiscal toolbar and removed the extra hard-coded lightning prefix | IMPLEMENTED | VERIFY |
| UI5-06 | `/organization` | Companies/Branches/Warehouses/Departments use legacy pill tabs | Migrated to shared wrapped product tabs | IMPLEMENTED | VERIFY |
| UI5-07 | `/settings` | Too many unrelated tabs in one horizontal strip; scrollbar and inconsistent product style | Split into two product-tab groups: normal/user settings + system/admin settings; tabs wrap instead of horizontal scrolling | IMPLEMENTED | VERIFY |
| UI5-08 | Global tab consistency audit | Other legacy tab implementations may remain elsewhere | Run `verify_bemo_uiux_phase5.py`; it writes a legacy-tab audit for remaining pages | AUDIT | VERIFY |

## Recommended QA sequence

1. Arabic Login: company code, username and password start visually from the right; English starts from the left.
2. Open 2–3 Arabic forms with normal text, numeric and date inputs and confirm alignment is consistent.
3. Settlement Periods in Dark + Light and verify every workflow label is readable.
4. Quality Inspection modal: scroll to Notes; verify Notes never sits under the action row; ESC still closes.
5. Production: both tabs use BEMO product tabs.
6. Fiscal Periods: year field and Create button have the same bottom/control alignment; only one lightning symbol at most comes from translated content.
7. Organization: all four tabs use the same BEMO tab pattern.
8. Settings: no horizontal scrollbar for the tab header at desktop width; normal and admin/system settings are separated into two wrapped groups.
9. Run the verification script and inspect the generated legacy-tab audit before closing UI5-08.
