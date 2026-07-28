# PROJECT_MAP.md - Bemo HR & Operations Platform

## System Flow & Architecture
- **Backend (`be/`)**: Spring Boot, PostgreSQL, App-Scoped JWT Auth, Multi-Role Authorization, Multi-Tenant (`@TenantId`), i18n database backend (`/api/v1/i18n/{locale}`).
- **Frontend (`fe/`)**: Angular 22 standalone, SCSS, Signals, Arabic RTL default (`ar-EG`/`en-US`), Excel exports.

---

## [COMPLETED & VERIFIED]
- App-scoped SaaS JWT Authentication & Session Timeout
- Multi-Role Authorization & Route Guards
- Database-backed i18n & `provideAppInitializer`
- Theme, Density & Language Preference Storage
- Employee Management, Categories, Biometric Imports
- Attendance Review & Custom Date Ranges
- Financial Accounts, Journal Entries, Bank Accounts & Fiscal Periods
- Operations: Procurement, Sales, Production, Quality, Parties & Inventory
- Audit Logs & User Management
- **Loading State & Skeleton Component** (Bemo logo skeleton, error retry button, zero premature Empty States)
- **Translation & Instant Locale Switching** (Instant RTL/LTR & language toggle without reload, Gross/Net Payout/Review translated, human-readable date formatting)
- **Payroll Pathway & Stepper** (5-step visual progress: Draft → Review → Approved & Locked → Accounting Posted → Disbursed, state-locked buttons, "صرف مرتب الموظف" button, impact confirmation dialogs)
- **Settings Page Reorganization** (4 Tabbed Sections: Appearance & Language | Session | Security | Reports & Export, unified "Save All Settings" action)
- **Sidebar Enhancements** (Collapsible workspace groups, pinned Favorites section with star toggle, Recently Used dynamic navigation history)
- **Visual Feedback & Touch Safety** (Toast notifications, `UnsavedChangesGuard` form protection, confirmation modals for financial/audit actions, minimum 40-44px touch targets)
- **Rich Contextual Empty States** (`EmptyStateComponent` with custom icons, clear guidance, and primary action buttons)

---

## [ORPHANS & PENDING]
*(None - All features and UX enhancements completed and verified)*

---

## [SYSTEM_FLOW Checklist]
- [x] Skeleton UI rendered on initial load across features
- [x] Dynamic reactive i18n translation without page reload
- [x] Formatted dates across all tables/views
- [x] Visual Payroll Stepper and explicit status flow guardrails
- [x] Tabbed Settings with unified save action
- [x] Collapsible Sidebar with Favorites & Recent Items
- [x] Global Toasts, Unsaved Form Guards & Action Confirmation Dialogs
- [x] High-contrast 40-44px touch targets & informative Empty States
