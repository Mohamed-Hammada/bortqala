# Frontend — HR & Operations Web Application (`fe/`)

Angular 22 standalone application built with signals, typed reactive forms, SCSS, Arabic RTL defaults, and a token-based design system. Zero external heavy UI component libraries.

---

## Core Capabilities & Architecture

1. **Design System & Theme Orchestration**:
   - Token-based design tokens using CSS variables (`--canvas`, `--surface`, `--surface-muted`, `--input-bg`, `--ink`, `--secondary-text`, `--gold`).
   - Seamless **Dark Mode** (`#0B0F14`) and **Light Mode** (`#F1F5F9`) support with high contrast and smooth CSS micro-interactions.

2. **Per-User Menu Access Control**:
   - `AuthService.hasMenuAccess(menuId)` enforces dynamic menu navigation visibility for assigned user menus.

3. **Attendance Review & Health Categories**:
   - Segmented view for 🟢 **Green** (Clean), 🟡 **Yellow** (Single Punch/Grace), and 🔴 **Red** (Critical/Absence) attendance tiers.
   - Bulk decision actions for single-punch approvals and missing-punch deductions.

4. **Localization & Date Handling**:
   - Database-backed dynamic i18n translation engine (`ar-EG` & `en-US`).
   - Verified via `npm run check:i18n`. All dates handled in Unix epoch milliseconds via `core/date.ts`.

---

## Command Reference

```powershell
# Start Angular development server (proxying /api to http://localhost:8080)
npm start

# Run i18n translation validation check
npm run check:i18n

# Build production bundle
npm run build
```
