# BEMO ERP UI/UX Phase 4

Reviewed commit: `b08a0e8dc81e46eb64a2a6e136a24a0057ad4dcb`

## Applied

- Restored compact per-role additional information on `/users`.
- Kept the full-width, user-first Role Directory.
- Migrated the complete `fe/src/app/features/workforce` style surface from fixed light neutral colors to BEMO semantic theme tokens.
- Workforce files changed by palette migration: **11**.
- CSS declarations semantically migrated: **260**.

## Files touched by workforce palette migration

- `fe\src\app\features\workforce\pages\advances\advances.component.ts`
- `fe\src\app\features\workforce\pages\categories\categories.component.ts`
- `fe\src\app\features\workforce\pages\contractor-accounts\contractor-accounts.component.ts`
- `fe\src\app\features\workforce\pages\contractors\contractors.component.ts`
- `fe\src\app\features\workforce\pages\dashboard\dashboard.component.ts`
- `fe\src\app\features\workforce\pages\labor-requests\labor-requests.component.ts`
- `fe\src\app\features\workforce\pages\manual-attendance\manual-attendance.component.ts`
- `fe\src\app\features\workforce\pages\reports-import\reports-import.component.ts`
- `fe\src\app\features\workforce\pages\settlement-periods\settlement-periods.component.ts`
- `fe\src\app\features\workforce\pages\workers\workers.component.ts`
- `fe\src\app\features\workforce\ui\contractor-settlement-detail-modal.component.ts`

## QA focus

- `/users`: open Roles, expand several roles, verify only the selected role shows page/access details.
- `/workforce/reports-import`: no white workflow/history cards in dark mode; headings and muted text remain readable.
- `/workforce/contractor-accounts`: title, table/card and buttons use the dark product palette.
- `/workforce/advances`: summary cards, policy area, table, modal helper panels and confirmation surfaces remain readable.
- Spot-check the remaining Workforce routes in both Dark and Light themes.
