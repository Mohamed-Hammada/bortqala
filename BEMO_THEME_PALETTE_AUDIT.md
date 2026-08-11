# BEMO Frontend Theme Palette Audit

This is a review report, not an automatic failure gate.

It flags feature CSS that still hard-codes a light surface, dark-only text, or light neutral border instead of semantic theme tokens.

## Summary by feature area

- **trade**: 12 suspect declarations
- **approvals**: 4 suspect declarations
- **change-password**: 3 suspect declarations
- **login**: 3 suspect declarations
- **settings**: 3 suspect declarations
- **payroll**: 2 suspect declarations
- **reports**: 1 suspect declarations
- **users**: 1 suspect declarations

## File findings

- `fe\src\app\features\approvals\pages\workflow-definitions\workflow-definitions.component.ts` — fixed white/light surface: **1**
- `fe\src\app\features\approvals\pages\workflow-definitions\workflow-definitions.component.ts` — fixed light neutral border: **3**
- `fe\src\app\features\change-password\change-password.page.scss` — fixed white/light surface: **1**
- `fe\src\app\features\change-password\change-password.page.scss` — fixed dark text: **2**
- `fe\src\app\features\login\login.page.scss` — fixed white/light surface: **1**
- `fe\src\app\features\login\login.page.scss` — fixed dark text: **2**
- `fe\src\app\features\payroll\ui\payroll-stepper.component.scss` — fixed white/light surface: **1**
- `fe\src\app\features\payroll\ui\payroll-stepper.component.scss` — fixed light neutral border: **1**
- `fe\src\app\features\reports\report-review.page.scss` — fixed dark text: **1**
- `fe\src\app\features\settings\settings.page.scss` — fixed white/light surface: **1**
- `fe\src\app\features\settings\settings.page.scss` — fixed dark text: **1**
- `fe\src\app\features\settings\translation-management.component.scss` — fixed white/light surface: **1**
- `fe\src\app\features\trade\procurement\procurement.page.scss` — fixed white/light surface: **5**
- `fe\src\app\features\trade\procurement\procurement.page.scss` — fixed dark text: **3**
- `fe\src\app\features\trade\procurement\procurement.page.scss` — fixed light neutral border: **4**
- `fe\src\app\features\users\users.page.scss` — fixed dark text: **1**

## Interpretation

- Fixed light surfaces often create the large white cards seen in Dark mode.
- Fixed dark text often creates headings or helper text that become nearly invisible on the dark canvas.
- Status/brand colors are intentionally not blanket-flagged because they require semantic review rather than mechanical replacement.
